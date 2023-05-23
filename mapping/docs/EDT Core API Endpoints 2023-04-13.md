# Known EDT Core API Endpoints

## Endpoints used, or ready for use, by ISL (JADE-CCM)

| API Name | HTTP Operation | HTTP Endpoint |
|---|---|---|
|GetVersion|GET|/api/v1/version|
|GetCases|GET|/api/v1/org-units/{orgUnitId}/cases|
|GetCustomFields|GET|/api/v1/org-units/{orgUnitId}/fields|
|CaseIdLookup|GET|/api/v1/org-units/{orgUnitId}/cases/{keyOrField}/id|
|CreateCase|POST|/api/v1/org-units/{orgUnitId}/cases|
|GetCase|GET|/api/v1/cases/{caseId}|
|UpdateCase|PUT|/api/v1/cases/{caseId}|
|ExportToCase|POST|/api/v1/cases/{caseId}/export-to-case/merge-case/{destCaseId}|
|SyncCaseUsers|POST|/api/v1/cases/{caseId}/case-users/sync|
|SyncUserCases|POST|/api/v1/org-units/{orgUnitId}/users/{userIdOrKey}/cases/sync|
|GetUserCases|GET|/api/v1/org-units/{orgUnitId}/users/{userIdOrKey}/cases|
|GetCaseGroups|GET|/api/v1/cases/{caseId}/groups|
|GetCaseGroup|GET|/api/v1/cases/{caseId}/groups/{groupId}|
|SyncCaseGroupMembers|POST|/api/v1/cases/{caseId}/groups/{groupId}/sync|
|CreatePerson|POST|/api/v1/org-units/{orgUnitId}/persons|
|GetActivePersons|GET|/api/v1/org-units/{orgUnitId}/persons|
|GetPerson|GET|/api/v1/org-units/{orgUnitId}/persons/{personIdOrKey}|
|UpdatePerson|PUT|/api/v1/org-units/{orgUnitId}/persons/{personIdOrKey}|
|AddCaseParticipant|POST|/api/v1/cases/{caseId}/participants|
|SyncCaseParticipants|POST|/api/v1/cases/{caseId}/participants/sync|
|GetCaseParticipants|GET|/api/v1/cases/{caseId}/participants|
|GetParticipantTypes|GET|/api/v1/org-units/{orgUnitId}/participant-types|
|GetRecords|GET|/api/v1/cases/{caseId}/records|
|CreateRecord|POST|/api/v1/cases/{caseId}/records|
|UploadRecordFile|PUT|/api/v1/cases/{caseId}/records/{recordId}/Native|
|UpdateRecord|PUT|/api/v1/cases/{caseId}/records/{idOrKey}|
|DeleteRecord|DELETE|/api/v1/cases/{caseId}/records/{idOrKey}|
|DeleteAllRecords|DELETE|/api/v1/cases/{caseId}/records|
|GetUsers|GET|/api/v1/users|
|CreateUser|POST|/api/v1/users|
|GetUser|GET|/api/v1/users/{userId}|
|GetUser|GET|/api/v1/users/key:{key}|
|UpdateUser|PUT|/api/v1/users|
|GetOuGroups|GET|/api/v1/org-units/{orgUnitId}/groups|
|AddUserToOuGroup|POST|/api/v1/org-units/{orgUnitId}/groups/{ouGroupId}/users|
|RemoveUserFromOuGroup|DELETE|/api/v1/org-units/{orgUnitId}/groups/{ouGroupId}/users/{userId}|
|RemoveUserFromOuGroup|DELETE|/api/v1/org-units/{orgUnitId}/groups/{ouGroupId}/users/key:{key}|
|GetUserOuGroups|GET|/api/v1/org-units/{orgUnitId}/users/{userIdOrKey}/groups|

## Endpoints DIAM will use or will likely use

Endpoints in this list which are not in the ISL list above have not reached “published” status yet. These endpoints are indicated by an asterisk after their name.

| API Name | HTTP Operation | HTTP Endpoint |
|---|---|---|
|GetVersion|GET|/api/v1/version|
|CaseIdLookup|GET|/api/v1/org-units/{orgUnitId}/cases/{keyOrField}/id|
|GetCase|GET|/api/v1/cases/{caseId}|
|GetCaseUsers*|GET|/api/v1/cases/{caseId}/case-users/{userId}|
|GetUserCases|GET|/api/v1/org-units/{orgUnitId}/users/{userId}/cases|
|GetCaseGroups|GET|/api/v1/cases/{caseId}/groups|
|GetCaseUserGroups*|GET|/api/v1/cases/{caseId}/case-users/{userId}/groups|
|RemoveUserFromCase*|DELETE|/api/v1/cases/{caseId}/case-users/remove/{userId}|
|AddUserToCase*|POST|/api/v1/cases/{caseId}/case-users/{userId}|
|CreateUser|POST|/api/v1/users|
|GetUser|GET|/api/v1/users/key:{userKey}|
|GetOuGroups|GET|/api/v1/org-units/{orgUnitId}/groups|
|AddUserToOuGroup|POST|/api/v1/org-units/{orgUnitId}/groups/{ouGroupId}/users|
|RemoveUserFromOuGroup|DELETE|/api/v1/org-units/{orgUnitId}/groups/{ouGroupId}/users/{userId}|
|RemoveUserFromOuGroup|DELETE|/api/v1/org-units/{orgUnitId}/groups/{ouGroupId}/users/key:{key}|
|GetUserOuGroups|GET|/api/v1/org-units/{orgUnitId}/users/{userIdOrKey}/groups|