FROM mongo:7

COPY mongo-keyfile /etc/mongo-keyfile
COPY init-configsvr.sh init-shard.sh /usr/local/bin/

RUN chmod 400 /etc/mongo-keyfile && chown mongodb:mongodb /etc/mongo-keyfile \
    && chmod +x /usr/local/bin/init-configsvr.sh /usr/local/bin/init-shard.sh

ENTRYPOINT []
