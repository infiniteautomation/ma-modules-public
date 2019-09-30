/**
 * Copyright 2019 Infinite Automation Systems Inc.
 * http://infiniteautomation.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const {createClient, login, uuid, delay} = require('@infinite-automation/mango-client/test/testHelper');
const client = createClient();
const DataPoint = client.DataPoint;
const DataSource = client.DataSource;
const csvParser = require('csv-parser');
const Readable = require('stream').Readable;
const path = require('path');
const fs = require('fs');

describe('Event detectors CSV format', function() {
    before('Login', function() {
        return login.call(this, client).then((...args) => {
            this.csvClient = createClient({
                defaultHeaders: {
                    Accept : 'text/csv'
                }
            });
            
            //Override to return strings
            const restRequest = this.csvClient.restRequest;
            this.csvClient.restRequest = function(optionsArg) {
                optionsArg.dataType = 'string';
                return restRequest.apply(this, arguments);
            };
           
            // copy the session cookie to the csv client
            Object.assign(this.csvClient.cookies, client.cookies);
        });
    });

    const newDataPoint = (xid, dsXid) => {
        return new DataPoint({
            xid: xid,
            enabled: true,
            name: 'Point values test',
            deviceName: 'Point values test',
            dataSourceXid : dsXid,
            pointLocator : {
                startValue : '0',
                modelType : 'PL.VIRTUAL',
                dataType : 'NUMERIC',
                changeType : 'NO_CHANGE',
                settable: true
            },
            textRenderer: {
                type: 'textRendererAnalog',
                format: '0.00',
                suffix: '',
                useUnitAsSuffix: false,
                unit: '',
                renderedUnit: ''
            },
            rollup: 'AVERAGE'
        });
    };
    
    const highLimitDetector = (xid, sourceId) => {
        return {
            xid: xid,
            sourceId: sourceId,
            name : "When true.",
            alarmLevel : 'URGENT',
            duration : {
                periods: 10,
                type: 'SECONDS'
            },
            limit: 10.0,
            resetLimit: 9.0,
            useResetLimit: true,
            notHigher: false,
            detectorType : "HIGH_LIMIT",
        };
    };
    
    const testPointXid1 = uuid();
    const testPointXid2 = uuid();
    const testDetectorXid1 = 'AAA' + uuid();
    const testDetectorXid2 = 'ZZZ' + uuid();
    
    before('Create a virtual data source and points', function() {

        this.ds = new DataSource({
            xid: uuid(),
            name: 'Mango client test',
            enabled: true,
            modelType: 'VIRTUAL',
            pollPeriod: { periods: 5, type: 'HOURS' },
            purgeSettings: { override: false, frequency: { periods: 1, type: 'YEARS' } },
            alarmLevels: { POLL_ABORTED: 'URGENT' },
            editPermission: null
        });
        
        return this.ds.save().then((savedDs) => {
            assert.strictEqual(savedDs.name, 'Mango client test');
            assert.isNumber(savedDs.id);
        }).then(() => {
            this.testPoint1 = newDataPoint(testPointXid1, this.ds.xid);
            this.testPoint2 = newDataPoint(testPointXid2, this.ds.xid);
            return Promise.all([this.testPoint1.save(), this.testPoint2.save()]);
        }).then(() => {
            this.ed1 = highLimitDetector(testDetectorXid1, this.testPoint1.id);
            this.ed2 = highLimitDetector(testDetectorXid2, this.testPoint2.id);
            return Promise.all([
                client.restRequest({
                    path: '/rest/v2/full-event-detectors',
                    method: 'POST',
                    data: this.ed1
                }).then(response => {
                    this.ed1.id = response.data.id;
                }),
                client.restRequest({
                    path: '/rest/v2/full-event-detectors',
                    method: 'POST',
                    data: this.ed2
                }).then(response => {
                    this.ed2.id = response.data.id;
                })
           ]);
        });
    });

    after('Deletes the new virtual data source and its points', function() {
        return this.ds.delete();
    });
    
    it('Can download csv file for both event detectors', function() {
        return this.csvClient.restRequest({
            path: `/rest/v2/full-event-detectors?in(xid,${this.ed1.xid},${this.ed2.xid})&sort(xid)`,
            method: 'GET'
        }).then(response => {
            const result = [];
            const s = new Readable();
            s.push(response.data);
            s.push(null);
            s.pipe(csvParser())
             .on('headers', function(headers){result.push(headers);})
             .on('data', function (data){
                 result.push(data);
             })
             .on('end', () => {
                 assert.isArray(result);
                 const headers = result.shift();
                 //assert.strictEqual(headers[0], "''");
                 assert.strictEqual(headers[1], 'action');
                 assert.strictEqual(headers[2], 'originalXid');
                 assert.strictEqual(headers[3], 'sourceId');
                 assert.strictEqual(result.length, 2);
                 assert.strictEqual(result[0].sourceId, String(this.ed1.sourceId));
                 assert.strictEqual(result[1].sourceId, String(this.ed2.sourceId));
                 assert.strictEqual(result[0].originalXid, String(this.ed1.xid));
                 assert.strictEqual(result[1].originalXid, String(this.ed2.xid));                 
             })
             .on('error', (error) => { assert.fail(error);});
        });
    });
    
    it('Can upload csv file for both event detectors', function() {
        this.timeout(5000);
        return this.csvClient.restRequest({
            path: `/rest/v2/full-event-detectors?in(xid,${this.ed1.xid},${this.ed2.xid})&sort(xid)`,
            method: 'GET',
            writeToFile: 'eventDetectors.csv'
        }).then(() => delay(1000)).then(response => {
            const uploadFileName = path.resolve('eventDetectors.csv');
            return this.csvClient.restRequest({
                path: `/rest/v2/full-event-detectors/bulk`,
                method: 'POST',
                headers: {
                    'Content-Type': 'text/csv;charset=UTF-8'
                },
                data: fs.readFileSync(uploadFileName)
            }).then(response => {
                return delay(1000).then(()=>{
                    return client.restRequest({
                        path: response.headers.location
                    }).then(response => {
                        assert.strictEqual(response.data.result.hasError, false);
                        assert.strictEqual(response.data.result.responses[0].xid, this.ed1.xid);
                        assert.strictEqual(response.data.result.responses[1].xid, this.ed2.xid);
                    });                    
                });
            });
        });
    });
    
    //TODO Test Upload to /bulk of modified detectors
});