export NS="cef5dd-dev"
export NAME="a-greeting-service-for-cef5dd-dev"
echo "services:
- name: $NAME
  host: ccm-justin-mock-app.$NS.svc
  tags: [ ns.$NS ]
  port: 443
  protocol: https
  retries: 0
  routes:
  - name: $NAME-route
    tags: [ ns.$NS ]
    hosts:
    - dems-adapter-dev.api.gov.bc.ca
    paths:
    - /
    methods:
    - GET
    strip_path: false
    https_redirect_status_code: 426
    path_handling: v0
" > greeting-api.yaml