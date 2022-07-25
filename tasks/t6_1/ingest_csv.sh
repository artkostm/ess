curl -XPUT 'http://localhost:9200/employee_info_v1' -H "Content-Type: application/json" -d '{"mappings":{"properties":{"upn":{"type":"keyword"},"first_name":{"type":"keyword"},"last_name":{"type":"keyword"},"managers":{"type":"text"}}}}'

curl -XPUT 'http://localhost:9200/_ingest/pipeline/parse_employee_csv' -H "Content-Type: application/json" -d '{"description":"Parsing test employee info","processors":[{"grok":{"field":"csv_row","ignore_missing":true,"patterns":["%{EMAILADDRESS:upn},%{WORD:first_name},%{WORD:last_name},%{DATA:managers}"]}},{"remove":{"field":"csv_row"}}]}'

while read f1
do
  curl -XPOST 'http://localhost:9200/employee_info_v1/_doc?pipeline=parse_employee_csv' -H "Content-Type: application/json" -d "{ \"csv_row\": \"$f1\" }"
done < "../t6_1_spark/src/test/resources/test_employee_no_header.csv"