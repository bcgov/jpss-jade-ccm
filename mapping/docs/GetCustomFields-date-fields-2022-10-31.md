Handlebars script to generate a markdown table from DEMS GetCustomFields json output

```
| Field Id | Field Name | Object Type | Data Type |
| --- | --- | --- | --- |
{{#each .}}
| {{id}} | {{name}} | {{objectType}} | {{dataType}} |
{{/each}}
```

Date and DateOnly custom fields as of Oct 31 2022

| id | name | objectType | dataType |
| --- | --- | --- | --- |
| 30 | Proposed App. Date (earliest) | Case | Date |
| 37 | Initial App. Date | Case | Date |
| 39 | Next App. Date | Case | Date |
| 41 | First Trial Date | Case | Date |
| 48 | Last JUSTIN Update | Case | Date |
| 55 | Limitation Date | Case | DateOnly |
| 60 | Date of Birth | PeopleOrgs | Date |
| 65 | Submit Date | Case | DateOnly |
| 66 | Sworn Date | Case | DateOnly |
| 67 | Offence Date (earliest) | Case | DateOnly |


All custom fields as of Oct 31 2022

| Field Id | Field Name | Object Type | Data Type |
| --- | --- | --- | --- |
| 1 | PartId | User | UniqueIdentifier |
| 3 | Agency File No. | Case | Text |
| 7 | Disclosure Status | Case | List |
| 9 | Assessment Crown | Case | Text |
| 12 | Agency File ID | Case | Text |
| 14 | Case Decision | Case | List |
| 15 | Court File Unique ID | Case | Text |
| 16 | Crown Election | Case | Text |
| 17 | Court File Level | Case | Text |
| 18 | Proposed Charges | Case | Text |
| 19 | Class | Case | Text |
| 20 | Designation | Case | Text |
| 23 | Court File No. | Case | Text |
| 24 | Initiating Agency | Case | Text |
| 25 | Investigating Officer | Case | Text |
| 26 | Proposed Crown Office | Case | Text |
| 27 | Court Home Registry | Case | Text |
| 28 | Case Flags | Case | MultiValueList |
| 30 | Proposed App. Date (earliest) | Case | Date |
| 31 | Proposed Process Type | Case | Text |
| 32 | RMS Processing Status | Case | Text |
| 33 | Assigned Legal Staff | Case | Text |
| 34 | Assigned Crown | Case | Text |
| 37 | Initial App. Date | Case | Date |
| 38 | Initial App. Date Reason | Case | Text |
| 39 | Next App. Date | Case | Date |
| 40 | Next App. Date Reason | Case | Text |
| 41 | First Trial Date | Case | Date |
| 42 | First Trial Date Reason | Case | Text |
| 43 | Assigned Crown Name | Case | Text |
| 44 | Court Home Registry Name | Case | Text |
| 45 | Charges | Case | Text |
| 47 | Accused Full Name | Case | Text |
| 48 | Last JUSTIN Update | Case | Date |
| 51 | Given Name 2 | PeopleOrgs | Text |
| 52 | Given Name 3 | PeopleOrgs | Text |
| 54 | Full Name | PeopleOrgs | Text |
| 55 | Limitation Date | Case | DateOnly |
| 56 | RCC Status | Case | List |
| 57 | Crown Office | Case | Text |
| 58 | Primary Agency File ID | Case | Text |
| 59 | Primary Agency File No. | Case | Text |
| 60 | Date of Birth | PeopleOrgs | Date |
| 61 | Court File Details | Case | Text |
| 65 | Submit Date | Case | DateOnly |
| 66 | Sworn Date | Case | DateOnly |
| 67 | Offence Date (earliest) | Case | DateOnly |
