FROM python:3.10

RUN apt-get update

COPY ../requirements.txt requirements.txt

RUN pip3 install -r requirements.txt

COPY . .
