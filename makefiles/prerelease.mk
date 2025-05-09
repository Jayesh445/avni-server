open-user-media-prerelease:
ifndef orgMedia
	@echo "Provde the orgMedia variable with org media prefix"
	exit 1
endif
	open "https://s3.console.aws.amazon.com/s3/buckets/prerelease-user-media?region=ap-south-1&prefix=$(orgMedia)/&showversions=false"

open-cognito-prerelease:
	open https://ap-south-1.console.aws.amazon.com/cognito/v2/idp/user-pools/ap-south-1_vfNvMvMk9/users?region=ap-south-1

start_server_prerelease: build_server
	-mkdir -p /tmp/openchs && sudo ln -s /tmp/openchs /var/log/avni_server
	AVNI_IDP_TYPE=cognito \
	OPENCHS_CLIENT_ID=$(OPENCHS_CLIENT_ID) \
	OPENCHS_USER_POOL=$(OPENCHS_USER_POOL) \
	OPENCHS_IAM_USER=$(OPENCHS_IAM_USER) \
	OPENCHS_IAM_USER_ACCESS_KEY=$(OPENCHS_IAM_USER_ACCESS_KEY) \
	OPENCHS_IAM_USER_SECRET_ACCESS_KEY=$(OPENCHS_IAM_USER_SECRET_ACCESS_KEY) \
	OPENCHS_BUCKET_NAME=prerelease-user-media \
  	OPENCHS_DATABASE_HOST=localhost OPENCHS_DATABASE_PORT=5433 \
    	java -jar avni-server-api/build/libs/avni-server-0.0.1-SNAPSHOT.jar

debug_server_prerelease: build_server
	-mkdir -p /tmp/openchs && sudo ln -s /tmp/openchs /var/log/avni_server
	AVNI_IDP_TYPE=cognito \
	OPENCHS_CLIENT_ID=$(OPENCHS_CLIENT_ID) \
	OPENCHS_USER_POOL=$(OPENCHS_USER_POOL) \
	OPENCHS_IAM_USER=$(OPENCHS_IAM_USER) \
	OPENCHS_IAM_USER_ACCESS_KEY=$(OPENCHS_IAM_USER_ACCESS_KEY) \
	OPENCHS_IAM_USER_SECRET_ACCESS_KEY=$(OPENCHS_IAM_USER_SECRET_ACCESS_KEY) \
	OPENCHS_BUCKET_NAME=prerelease-user-media \
  	OPENCHS_DATABASE_HOST=localhost OPENCHS_DATABASE_PORT=5433 \
    	java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -jar avni-server-api/build/libs/avni-server-0.0.1-SNAPSHOT.jar

tunnel_prerelease_server_for_debug:
	ssh avni-prerelease -L 5005:127.0.0.1:5005