SHELL     = /bin/bash
OUTDIR    = $(shell pwd)
CONT_NAME = flopoco:latest

$(OUTDIR)/.docker_built: flopoco.Dockerfile
	mkdir -p $(OUTDIR)
	docker build -f flopoco.Dockerfile -t $(CONT_NAME) .
	touch $(OUTDIR)/.docker_built

.PHONY: build
build: $(OUTDIR)/.docker_built
