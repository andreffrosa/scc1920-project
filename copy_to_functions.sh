SERVERLESS_DIR=../scc1920-project-serverless
mkdir -p $SERVERLESS_DIR/src/main/resources && cp -r ./src/main/resources/* $SERVERLESS_DIR/src/main/resources
mkdir -p $SERVERLESS_DIR/src/scc/resources && cp -r ./src/scc/resources/* $SERVERLESS_DIR/src/scc/resources
mkdir -p $SERVERLESS_DIR/src/scc/models && cp -r ./src/scc/models/* $SERVERLESS_DIR/src/scc/models
mkdir -p $SERVERLESS_DIR/src/scc/storage && cp -r ./src/scc/storage/* $SERVERLESS_DIR/src/scc/storage
mkdir -p $SERVERLESS_DIR/src/scc/utils && cp -r ./src/scc/utils/* $SERVERLESS_DIR/src/scc/utils
