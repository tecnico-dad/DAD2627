#!/usr/bin/env bash
set -e

# Distribution/Architecture specific flags
JAVA_DIST="x64"
PROTOC_DIST="x86_64"

# Define versions
JAVA_RELEASE=22
JAVA_VERSION="22.0.2"
MAVEN_VERSION=3.8.4
PROTOC_VERSION=3.12.0
INSTALL_DIR="$(pwd)/dev_env"

mkdir -p "$INSTALL_DIR"
cd "$INSTALL_DIR"

# ---- Download and setup Java 22 ----
echo "Downloading Java $JAVA_VERSION..."
JAVA_DIR="jdk-$JAVA_VERSION"
if [ ! -d "$JAVA_DIR" ]; then
    curl -L -o java.tar.gz "https://download.oracle.com/java/$JAVA_RELEASE/archive/jdk-${JAVA_VERSION}_linux-${JAVA_DIST}_bin.tar.gz"
    tar -xzf java.tar.gz
    rm java.tar.gz
fi

# ---- Download and setup Maven ----
echo "Downloading Maven $MAVEN_VERSION..."
MAVEN_DIR="apache-maven-$MAVEN_VERSION"
if [ ! -d "$MAVEN_DIR" ]; then
    curl -L -o maven.tar.gz "https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz"
    tar -xzf maven.tar.gz
    rm maven.tar.gz
fi

# ---- Download and setup Protoc ----
echo "Downloading Protoc $PROTOC_VERSION..."
PROTOC_DIR="protoc-$PROTOC_VERSION"
if [ ! -d "$PROTOC_DIR" ]; then
    mkdir -p "$PROTOC_DIR"
    curl -L -o protoc.zip "https://github.com/protocolbuffers/protobuf/releases/download/v$PROTOC_VERSION/protoc-$PROTOC_VERSION-linux-$PROTOC_DIST.zip"
    unzip protoc.zip -d "$PROTOC_DIR"
    rm protoc.zip
fi

# ---- Create environment export script ----
ENV_SCRIPT="$INSTALL_DIR/env.sh"
cat > "$ENV_SCRIPT" <<EOF
# Source this script to set environment
export JAVA_HOME="$INSTALL_DIR/$JAVA_DIR"
export MAVEN_HOME="$INSTALL_DIR/$MAVEN_DIR"
export PROTOC_HOME="$INSTALL_DIR/$PROTOC_DIR"
export PATH="\$JAVA_HOME/bin:\$MAVEN_HOME/bin:\$PROTOC_HOME/bin:\$PATH"
EOF

echo
echo "✅ Environment setup complete."
echo "👉 To use it, run:"
echo "    source \"$ENV_SCRIPT\""
