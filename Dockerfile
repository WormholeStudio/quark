FROM rust:1.53
RUN apt-get update
RUN apt-get install -y clang
RUN apt-get install -y default-jdk

RUN cargo install --git https://github.com/starcoinorg/starcoin move-cli --branch master