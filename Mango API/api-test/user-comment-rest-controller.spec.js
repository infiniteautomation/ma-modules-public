/**
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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

const {createClient, login, uuid, assertValidationErrors, noop} = require('@infinite-automation/mango-module-tools/test-helper/testHelper');
const client = createClient();

// Mango REST V1 API - User Comments
describe('user-comment-rest-controller', function() {
    before('Login', function() { return login.call(this, client); });

    const noCreate = [];
    
    beforeEach('Test setup', function() {
        this.currentTest.xid = uuid();
        // common test setup, e.g. create a VO object
        this.currentTest.expectedResult = {
            xid: this.currentTest.xid,
            comment: 'string',
            commentType: 'POINT',
            referenceId: 1,
            timestamp: null,
            //userId: 1,
            username: 'admin'
        };
        
        if (!noCreate.includes(this.currentTest)) {
            return client.restRequest({
                method: 'POST',
                path: '/rest/latest/comments',
                data: this.currentTest.expectedResult
            });
        }
    });
    
    afterEach('Test teardown', function() {
        // common test teardown, e.g. delete a VO object
        return client.restRequest({
            method: 'DELETE',
            path: `/rest/latest/comments/${this.currentTest.xid}`,
        }).catch(noop);
    });
    
    // Query User Comments - 
    it('GET /rest/latest/comments', function() {
        return client.restRequest({
            method: 'GET',
            path: `/rest/latest/comments?limit(10)`,
        }).then(response => {
            // OK
            assert.strictEqual(response.status, 200);
            // MODEL: UserCommentQueryResult
            assert.isObject(response.data, 'data');
            assert.isArray(response.data.items, 'data.items');
            response.data.items.forEach((item, index) => {
                // MODEL: UserCommentModel
                assert.isObject(item, 'data.items[]');
                assert.isString(item.comment, 'data.items[].comment');
                assert.isString(item.commentType, 'data.items[].commentType');
                assert.include(["POINT","EVENT","JSON_DATA"], item.commentType, 'data.items[].commentType');
                // DESCRIPTION: ID of object in database
                assert.isNumber(item.id, 'data.items[].id');
                assert.isNumber(item.referenceId, 'data.items[].referenceId');
                assert.isNumber(item.timestamp, 'data.items[].timestamp');
                assert.isNumber(item.userId, 'data.items[].userId');
                assert.isString(item.username, 'data.items[].username');
                // DESCRIPTION: XID of object
                assert.isString(item.xid, 'data.items[].xid');
                // END MODEL: UserCommentModel
            });
            assert.isNumber(response.data.total, 'data.total');
            // END MODEL: UserCommentQueryResult
        });
    });
    
    // Query User Comments - 
    it('GET /rest/latest/comments using POINT comment type in query', function() {
        return client.restRequest({
            method: 'GET',
            path: `/rest/latest/comments?commentType=POINT&limit(1)`,
        }).then(response => {
            // OK
            assert.strictEqual(response.status, 200);
            assert.strictEqual(response.data.items.length, 1);
            assert.strictEqual(response.data.items[0].xid, this.test.xid);
            // MODEL: UserCommentQueryResult
            assert.isObject(response.data, 'data');
            assert.isArray(response.data.items, 'data.items');
            response.data.items.forEach((item, index) => {
                // MODEL: UserCommentModel
                assert.isObject(item, 'data.items[]');
                assert.isString(item.comment, 'data.items[].comment');
                assert.isString(item.commentType, 'data.items[].commentType');
                assert.include(["POINT","EVENT","JSON_DATA"], item.commentType, 'data.items[].commentType');
                // DESCRIPTION: ID of object in database
                assert.isNumber(item.id, 'data.items[].id');
                assert.isNumber(item.referenceId, 'data.items[].referenceId');
                assert.isNumber(item.timestamp, 'data.items[].timestamp');
                assert.isNumber(item.userId, 'data.items[].userId');
                assert.isString(item.username, 'data.items[].username');
                // DESCRIPTION: XID of object
                assert.isString(item.xid, 'data.items[].xid');
                // END MODEL: UserCommentModel
            });
            assert.isNumber(response.data.total, 'data.total');
            // END MODEL: UserCommentQueryResult
        });
    });

    // Create New User Comment - 
    noCreate[noCreate.length] = it('POST /rest/latest/comments', function() {
        const requestBody =
        { // title: UserCommentModel
            comment: 'string',
            commentType: 'POINT',
            referenceId: 1,
            timestamp: null,
            //userId: 0,
            username: 'admin',
            xid: this.test.xid
        };
        const params = {
            model: requestBody // in = body, description = User Comment to save, required = true, type = , default = , enum = 
        };
        
        return client.restRequest({
            method: 'POST',
            path: `/rest/latest/comments`,
            data: requestBody
        }).then(response => {
            // OK
            assert.strictEqual(response.status, 201);
            // MODEL: UserCommentModel
            assert.isObject(response.data, 'data');
            assert.isString(response.data.comment, 'data.comment');
            assert.isString(response.data.commentType, 'data.commentType');
            assert.include(["POINT","EVENT","JSON_DATA"], response.data.commentType, 'data.commentType');
            // DESCRIPTION: ID of object in database
            assert.isNumber(response.data.id, 'data.id');
            assert.isNumber(response.data.referenceId, 'data.referenceId');
            assert.isNumber(response.data.timestamp, 'data.timestamp');
            assert.isNumber(response.data.userId, 'data.userId');
            assert.isString(response.data.username, 'data.username');
            // DESCRIPTION: XID of object
            assert.isString(response.data.xid, 'data.xid');
            // END MODEL: UserCommentModel
        });
    });

    // Get user comment by xid - Returns the user comment specified by the given xid
    it('GET /rest/latest/comments/{xid}', function() {
        const params = {
            xid: this.test.xid // in = path, description = Valid xid, required = true, type = string, default = , enum = 
        };
        
        return client.restRequest({
            method: 'GET',
            path: `/rest/latest/comments/${params.xid}`,
        }).then(response => {
            // OK
            assert.strictEqual(response.status, 200);
            // MODEL: UserCommentModel
            assert.isObject(response.data, 'data');
            assert.isString(response.data.comment, 'data.comment');
            assert.isString(response.data.commentType, 'data.commentType');
            assert.include(["POINT","EVENT","JSON_DATA"], response.data.commentType, 'data.commentType');
            // DESCRIPTION: ID of object in database
            assert.isNumber(response.data.id, 'data.id');
            assert.isNumber(response.data.referenceId, 'data.referenceId');
            assert.isNumber(response.data.timestamp, 'data.timestamp');
            assert.isNumber(response.data.userId, 'data.userId');
            assert.isString(response.data.username, 'data.username');
            // DESCRIPTION: XID of object
            assert.isString(response.data.xid, 'data.xid');
            // END MODEL: UserCommentModel
            assert.strictEqual(response.data.comment, this.test.expectedResult.comment, 'data.comment');
        });
    });

    // Updates a user comment - 
    it('PUT /rest/latest/comments/{xid}', function() {
        const requestBody =
        { // title: UserCommentModel
            xid: this.test.xid,
            comment: 'new comment',
            commentType: 'POINT',
            referenceId: 1,
            timestamp: null,
            //userId: 0,
            username: 'admin'
        };
        const params = {
            model: requestBody, // in = body, description = model, required = true, type = , default = , enum = 
            xid: this.test.xid // in = path, description = xid, required = true, type = string, default = , enum = 
        };
        
        return client.restRequest({
            method: 'PUT',
            path: `/rest/latest/comments/${params.xid}`,
            data: requestBody
        }).then(response => {
            // OK
            assert.strictEqual(response.status, 200);
            // MODEL: UserCommentModel
            assert.isObject(response.data, 'data');
            assert.isString(response.data.comment, 'data.comment');
            assert.isString(response.data.commentType, 'data.commentType');
            assert.include(["POINT","EVENT","JSON_DATA"], response.data.commentType, 'data.commentType');
            // DESCRIPTION: ID of object in database
            assert.isNumber(response.data.id, 'data.id');
            assert.isNumber(response.data.referenceId, 'data.referenceId');
            assert.isNumber(response.data.timestamp, 'data.timestamp');
            assert.isNumber(response.data.userId, 'data.userId');
            assert.isString(response.data.username, 'data.username');
            // DESCRIPTION: XID of object
            assert.isString(response.data.xid, 'data.xid');
            // END MODEL: UserCommentModel
            assert.strictEqual(response.data.comment, 'new comment', 'data.comment');
        }, error => {
            assertValidationErrors([''], error);
        });
    });

    // Delete A User Comment by XID - 
    it('DELETE /rest/latest/comments/{xid}', function() {
        const params = {
            xid: this.test.xid // in = path, description = xid, required = true, type = string, default = , enum = 
        };
        
        return client.restRequest({
            method: 'DELETE',
            path: `/rest/latest/comments/${params.xid}`,
        }).then(response => {
            // OK
            assert.strictEqual(response.status, 200);
            // MODEL: UserCommentModel
            assert.isObject(response.data, 'data');
            assert.isString(response.data.comment, 'data.comment');
            assert.isString(response.data.commentType, 'data.commentType');
            assert.include(["POINT","EVENT","JSON_DATA"], response.data.commentType, 'data.commentType');
            // DESCRIPTION: ID of object in database
            assert.isNumber(response.data.id, 'data.id');
            assert.isNumber(response.data.referenceId, 'data.referenceId');
            assert.isNumber(response.data.timestamp, 'data.timestamp');
            assert.isNumber(response.data.userId, 'data.userId');
            assert.isString(response.data.username, 'data.username');
            // DESCRIPTION: XID of object
            assert.isString(response.data.xid, 'data.xid');
            // END MODEL: UserCommentModel
        });
    });

});
