{%- if not salt['pillar.get']('susemanager:distupgrade:dryrun', False) %}
{%- if grains['os_family'] == 'Suse' and grains['osmajorrelease']|int > 15 and "opensuse" not in grains['oscodename']|lower %}

add-uptime-tracking-repos:
  pkgrepo.managed:
    - humanname: SUSE uptime tracker Repo
    - baseurl: http://download.opensuse.org/repositories/systemsmanagement:/SCC/15.5/
    - gpgkey: http://download.opensuse.org/repositories/systemsmanagement:/SCC/15.5/repodata/repomd.xml.key
    - gpgcheck: 1
    - gpgautoimport: True

install-suse-uptime-tracker-pkgs:
  pkg.installed:
    - pkgs:
      - suse-uptime-tracker
      - openssh-server
    - require:
      -  add-uptime-tracking-repos

enable-suse-uptime-tracker.timer:
  service.running:
    - name: suse-uptime-tracker.timer
    - enable: True
    - require:
      -  install-suse-uptime-tracker-pkgs

dump-uptime-info:
  cmd.run:
    - name: cat /etc/zypp/suse-uptime.log
    - onlyif: test -f /etc/zypp/suse-uptime.log
    - require:
      -  enable-suse-uptime-tracker.timer

{%- endif %}
{%- endif %}
