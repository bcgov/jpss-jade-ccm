

## Aug 4 - Outstanding Tasks Relating to Create Case

1. JUSTIN Adapter
   1. Process JUSTIN events
      1. Switch from reading from file to reading from newBatch endpoint
      2. Iterate through events
      3. Process AGEN_FILE and AUTH_LIST events, and skip the rest
      4. Mark each event as processed successfully
      5. Create a business event for every AGEN_FILE (type = "caseChanged") event and every AUTH_LIST (type = "caseAccessListChanged") event
   2. Process getCaseData calls
      1. Upgrade to '2022-08-04' JSON version
2. Notification Service
   1. Process caseChanged event
      1. Log event
      2. Call lookupService.getCaseExists
      3. Produce caseCreated or caseUpdated events
   2. Process caseCreated event
      1. Log event
      2. Call lookupService.getCaseData
      3. Call demsAdapter.createCourtCase
   3. Process caseUpdated event
      1. Log event
   4. Process caseAccessListChanged event
3. Lookup Service
   1. Process getCaseExists calls
      1. Call demsAdapter.getCaseExists
   2. Process getCaseData calls
      2. Call justinAdapter.getCaseData
4. DEMS Adapter
   1. Process getCaseExists calls