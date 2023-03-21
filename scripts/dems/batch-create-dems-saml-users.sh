#!/bin/bash

function usage {
  local script_name=$(basename "$1")
  echo "Usage: $script_name -hfutx"
  echo "  -h Usage"
  echo "  -f <csv-file>: Path to CSV file"
  echo "  -u <endpoint-url>: Create user endpoint URL"
  echo "  -t <oauth-token>: Oauth token"
  echo "  -x Execute.  If missing, the script will perform a dry run instead of actually creating users."
}

function help {
    usage $1
    echo
    echo "Sample CSV file:"
    echo "----------------"
    echo "PART_ID,PART_USER_ID,IDDT_SURNAME_NM,IDDT_GIVEN_1_NM,AGEN_AGENCY_NM,GRANTED_ROLE"
    echo "170317.0877,TSTDEMSA01,Foley,Taha,Kelowna Crown Counsel,JRS_BASE_LAWYER"
}

# parse command-line arguments
while getopts ":hf:u:t:x" opt; do
  case $opt in
    h)
      help "$0"
      exit 0
      ;;
    f)
      CSV_FILE="$OPTARG"
      ;;
    u)
      ENDPOINT_URL="$OPTARG"
      ;;
    t)
      OAUTH_TOKEN="$OPTARG"
      ;;
    x)
      EXECUTE="Y"
      ;;
    \?)
      echo "Invalid option -$OPTARG" >&2
      usage "$0"
      exit 1
      ;;
    :)
      echo "Option -$OPTARG requires an argument." >&2
      usage "$0"
      exit 1
      ;;
  esac
done

# check if the CSV file was provided
if [ -z "$CSV_FILE" ]; then
  echo "Error: CSV file location not provided."
  usage "$0"
  exit 1
fi

# check if the CSV file exists
if [ ! -f "$CSV_FILE" ]; then
  echo "Error: File '$CSV_FILE' not found."
  exit 1
fi

# check if the endpoint URL was provided
if [ -z "$ENDPOINT_URL" ]; then
  echo "Error: create user endpoint URL not provided."
  usage "$0"
  exit 1
fi

# check if the endpoint URL is valid 
if ! [[ "$ENDPOINT_URL" =~ ^https?://[a-z0-9-]+(\.[a-z0-9-]+)+([/?].*)?$ ]]; then
  echo "Error: URL provided is not valid - \"$ENDPOINT_URL\""
  usage "$0"
  exit 1
fi

# check if the oauth token was provided
if [ -z "$OAUTH_TOKEN" ]; then
  echo "Error: create user endpoint token not provided."
  usage "$0"
  exit 1
fi

INDEX=1
TOTAL_STRING=$(cat "$CSV_FILE" | { echo "$(cat)"; } | grep  ',' | tail -n +2 | wc -l)
TOTAL=$((TOTAL_STRING))

# loop through each row in the CSV file
while IFS=',' read -r PART_ID PART_USER_ID IDDT_SURNAME_NM IDDT_GIVEN_1_NM AGEN_AGENCY_NM GRANTED_ROLE; do
    # do something with each row

    PART_USER_ID_LOWER=$(echo "$PART_USER_ID" | tr '[:upper:]' '[:lower:]')
    FULL_NAME=$(echo "$IDDT_GIVEN_1_NM $IDDT_SURNAME_NM")

    echo "Creating user $INDEX of $TOTAL ($FULL_NAME) ..."

    CREATE_USER_BODY=$(cat <<EOF
{
  "key": "$PART_ID",
  "username": "$PART_USER_ID_LOWER@adfs",
  "fullname": "$FULL_NAME",
  "email": "$PART_USER_ID_LOWER@placeholder.email",
  "accountType": "SAML",
  "role": "User",
  "isActive": false
}
EOF
)

    echo "$CREATE_USER_BODY"

    if [ -z "$EXECUTE" ]; then
      echo "Dry run only; user not created. Add the '-x' option to execute user creation."
    else
      response=$(curl -isb --output /dev/null \
      -H "Authorization: Bearer $OAUTH_TOKEN" \
      -H "Content-Type: application/json" \
      -X POST \
      -d "$CREATE_USER_BODY" \
      "$ENDPOINT_URL")

      response_code=$(echo "$response" | head -n 1 | awk '{print $2}')

      if [ "$response_code" = "201" ]; then
        echo "User created successfully."
      else
        echo "User creation failed:"
        echo "$response"
        echo
        echo "... execution terminated with error while processing user record $INDEX of $TOTAL ($FULL_NAME)."
        exit 1
      fi
    fi

    # newline
    echo

    ((INDEX++))
done < <(cat "$CSV_FILE" | { echo "$(cat)"; } | grep  ',' | tail -n +2)
