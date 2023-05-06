FROM grafana/grafana:latest

USER root

RUN apk update && apk add curl

# INIT #
COPY ./grafana.ini /etc/grafana/grafana.ini

# SETTINGS #
COPY ./provisioning /etc/grafana/provisioning
#ENV DS_LOKI=ds_loki_id

# LOAD DASHBOARD #
COPY ./dashboards /var/lib/grafana/dashboards

#ENV GF_DASHBOARDS_DEFAULT_HOME_DASHBOARD_PATH=/var/lib/grafana/dashboards/grafana-smg.json

HEALTHCHECK --start-period=10s --interval=5s --timeout=3s \
  CMD curl --fail -X POST -H 'Content-Type: application/json' \
  --user $GF_SECURITY_ADMIN_USER:$GF_SECURITY_ADMIN_PASSWORD http://localhost:3000/api/admin/users \
  --data '{"name": "User", "email": "user@user.com", "login": "user", "password": "user123"}' || exit 1

#USER grafana
