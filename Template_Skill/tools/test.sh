#!/bin/sh



gradle appStart &

until echo exit | nc localhost 8080; do sleep 5; done

run_test() {
    curl -s http://localhost:8080/Template_Skill/ssunews --data-binary "${1}" | json_pp
}

run_test '{
    "session": {
        "sessionId": "SessionId.SOMEUUID",
        "application": {
            "applicationId": "amzn1.ask.skill.SOMEUUID"
        },
        "attributes": {},
        "user": {
            "userId": "amzn1.ask.account.SUPERSECRETLONGSTRING"
        },
        "new": true
    },
    "request": {
        "type": "IntentRequest",
        "requestId": "EdwRequestId.SOMEUUID",
        "locale": "en-US",
        "timestamp": "2016-10-19T21:26:10Z",
        "intent": {
            "name": "NextEventIntent",
            "slots": {}
        }
    },
    "version": "1.0"
}'

run_test '{
    "session": {
        "sessionId": "SessionId.SOMEUUID",
        "application": {
            "applicationId": "amzn1.ask.skill.SOMEUUID"
        },
        "attributes": {},
        "user": {
            "userId": "amzn1.ask.account.SUPERSECRETLONGSTRING"
        },
        "new": true
    },
    "request": {
        "type": "IntentRequest",
        "requestId": "EdwRequestId.SOMEUUID",
        "locale": "en-US",
        "timestamp": "2016-10-19T21:26:10Z",
        "intent": {
            "name": "GetEventsOnDateIntent",
            "slots": {
                "date": {
                    "name": "date",
                    "value": "2016-11-14"
                }
            }
        }
    },
    "version": "1.0"
}'

run_test '{
    "session": {
        "sessionId": "SessionId.SOMEUUID",
        "application": {
            "applicationId": "amzn1.ask.skill.SOMEUUID"
        },
        "attributes" : {
            "savedDate" : {
                "end" : "2016-11-15",
                "begin" : "2016-11-14"
            },
            "stateId" : "LIST_TOO_LONG"
        },
        "user": {
            "userId": "amzn1.ask.account.SUPERSECRETLONGSTRING"
        },
        "new": false
    },
    "request": {
        "type": "IntentRequest",
        "requestId": "EdwRequestId.SOMEUUID",
        "locale": "en-US",
        "timestamp": "2016-10-19T21:26:10Z",
        "intent": {
            "name": "SportsCategoryIntent",
            "slots": {}
        }
    },
    "version": "1.0"
}'

gradle appStop
