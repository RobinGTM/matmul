FROM debian:latest as builder

# Prerequisites
RUN apt update
RUn apt install -y git make g++ cmake \
    ninja-build liblapacke-dev \
    libsollya-dev dh-autoreconf \
    flex libboost-all-dev \
    pkg-config libtbb-dev flex
WORKDIR /
# Clone repo
RUN git clone https://gitlab.com/flopoco/flopoco.git flopoco
WORKDIR /flopoco

# Make flopoco
RUN make

COPY flopoco.entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

WORKDIR /output

# Entrypoint
ENTRYPOINT [ "/entrypoint.sh" ]
