FROM maven:3.6.3-jdk-11-slim


## Install Node.js
RUN sudo apt-get update
RUN sudo apt install nodejs
RUN sudo apk install yarn
RUN sudo apk install git

# Install deptrim
RUN git clone https://github.com/castor-software/deptrim.git
RUN cd deptrim;  mvn install -Dmaven.test.skip;

# Create directories
RUN mkdir "/home/deptrim"
RUN mkdir "deptrim-experiments-resources/libraries"
COPY dataset /home/deptrim


# Exectute deptrim
WORKDIR /home/deptrim
RUN cd src; yarn install; node 4_running_deptrim_on_libraries.js

#ENTRYPOINT [ "./main.py" ]