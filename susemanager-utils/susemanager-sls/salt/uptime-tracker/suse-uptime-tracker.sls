{%- if grains['os_family'] == 'Suse' %}

dump-uptime-info:
  cmd.run:
    - name: cat /etc/zypp/suse-uptime.log
    - onlyif: test -f /etc/zypp/suse-uptime.log

{%- endif %}

