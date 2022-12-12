FROM maven:3.8.3-openjdk-17

RUN apt-get update
RUN apt-get install -y git

# make a directory for the app
RUN mkdir -p /usr/src/app

# set the working directory
WORKDIR /usr/src/app

# copy the .sh files
COPY ./pipeline.sh /usr/src/app/pipeline.sh
COPY ./run_pipeline.sh /usr/src/app/run-pipeline.sh

# make .sh files executable
RUN chmod +x /usr/src/app/pipeline.sh
RUN chmod +x /usr/src/app/run-pipeline.sh

# run the pipeline
CMD ["/usr/src/app/run-pipeline.sh"]






