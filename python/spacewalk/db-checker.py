#!/usr/bin/python
#  pylint: disable=missing-module-docstring,invalid-name
#
# Copyright (c) 2008--2016 Red Hat, Inc.
#
# This software is licensed to you under the GNU General Public License,
# version 2 (GPLv2). There is NO WARRANTY for this software, express or
# implied, including the implied warranties of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
# along with this software; if not, see
# http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
#
# Red Hat trademarks are not licensed under GPLv2. No permission is
# granted to use or replicate Red Hat trademarks that are incorporated
# in this software or its documentation.
#

import os
import sys
from uyuni.common import usix

_topdir = os.path.abspath(os.path.dirname(sys.argv[0]))
if _topdir not in sys.path:
    sys.path.append(_topdir)

# pylint: disable-next=wrong-import-position
from spacewalk.server import rhnSQL


def main():
    rhnSQL.initDB()

    # pylint: disable-next=undefined-variable
    if not args:
        print("No module specified")
        return 0

    if "." not in sys.path:
        sys.path.append(".")

    g = globals()

    # pylint: disable-next=undefined-variable
    for module_name in args:
        # pylint: disable-next=consider-using-f-string
        print(("Checking module %s" % module_name))
        pmn = proper_module_name(module_name)
        try:
            m = __import__(pmn)
            g[module_name] = m
        except ImportError:
            e = sys.exc_info()[1]
            # pylint: disable-next=consider-using-f-string
            print(("Unable to import module %s: %s" % (module_name, e)))
            continue

        comps = pmn.split(".")
        for c in comps[1:]:
            m = getattr(m, c)

        for mod, name, statement in get_class_instances(m, rhnSQL.Statement):
            try:
                rhnSQL.prepare(statement)
            except rhnSQL.SQLStatementPrepareError:
                e = sys.exc_info()[1]
                # pylint: disable-next=consider-using-f-string
                print(("Error: %s.%s: %s" % (mod.__name__, name, e)))


def proper_module_name(module_name):
    suffix = ".py"
    if module_name.endswith(suffix):
        module_name = module_name[: -len(suffix)]

    return os.path.normpath(module_name).replace("/", ".")


_objs_seen = {}


def get_class_instances(obj, class_obj):
    if not hasattr(obj, "__dict__"):
        return []
    id_obj = id(obj)
    if id_obj in _objs_seen:
        return []
    _objs_seen[id_obj] = None
    result = []
    for k, v in list(obj.__dict__.items()):
        if isinstance(v, class_obj):
            result.append((obj, k, v))
        elif isinstance(v, usix.ClassType):
            result.extend(get_class_instances(v, class_obj))
    return result


if __name__ == "__main__":
    sys.exit(main() or 0)
