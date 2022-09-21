# registry-regulation-management

This service provides API for batch-import process: import/export files with user data, retrieving files info 

### Related components:
* `ddm-ceph-client` - service, which provides methods for working with storage

### Local development:
###### Prerequisites:
* Ceph/S3-like storage is configured and running
* Vault service is configured and running

###### Configuration:
Check `src/main/resources/application-local.yaml` and replace if needed:
  * *-ceph properties with your ceph storage values
  * vault properties with your Vault values
Check `src/main/resources/bootstrap.yaml` and replace if needed:
  * vault properties with your Vault values for 'local' profile

###### Steps:
1. (Optional) Package application into jar file with `mvn clean package`
2. Add `--spring.profiles.active=local` to application run arguments
3. Run application with your favourite IDE or via `java -jar ...` with jar file, created above

###### RestAPI documentation generation

To generate document describing RestAPI definition separate maven profile should be used called _generate-rest-api-docs_. Call `mvn -Pgenerate-rest-api-docs clean install` to generate rest api documentation from scratch. After this generated documentation should be commited and pushed into git together with potential other changes about RestAPI.  


### License
registry-regulation-management is Open Source software released under the Apache 2.0 license.
