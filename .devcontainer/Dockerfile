FROM mcr.microsoft.com/devcontainers/java:1-21-bullseye

RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && apt-get -y install --no-install-recommends \
        postgresql-client \
        curl \
        wget \
        git \
        vim \
        tree \
    && apt-get clean -y && rm -rf /var/lib/apt/lists/*

# Kotlin CLI (optional, for scripting)
ENV KOTLIN_VERSION=2.3.20
RUN curl -sL "https://github.com/JetBrains/kotlin/releases/download/v${KOTLIN_VERSION}/kotlin-compiler-${KOTLIN_VERSION}.zip" -o /tmp/kotlin.zip \
    && unzip -d /opt /tmp/kotlin.zip \
    && ln -s /opt/kotlinc/bin/kotlin /usr/local/bin/kotlin \
    && ln -s /opt/kotlinc/bin/kotlinc /usr/local/bin/kotlinc \
    && rm /tmp/kotlin.zip

RUN mkdir -p /workspace && chown vscode:vscode /workspace

USER vscode
WORKDIR /workspace
