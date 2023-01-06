FROM alpine:latest

RUN apk update \
    && apk add --no-cache git maven \
    && apk add --no-cache --upgrade bash \
    && mkdir -p /home/app

# set the working directory
WORKDIR /usr/src/app

# copy the .sh files
COPY ./pipeline.sh /home/app/pipeline.sh
COPY ./run_pipeline.sh /home/app/run-pipeline.sh

# make .sh files executable
RUN chmod +x /home/app/pipeline.sh \
    && chmod +x /home/app/run-pipeline.sh

# run the pipeline
CMD ["/home/app/run-pipeline.sh"]

# docker build -t pipeline .
# docker build --no-cache -t pipeline .
# docker run -it pipeline