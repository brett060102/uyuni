/*
 * Copyright (c) 2024 SUSE LLC
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */

package com.suse.manager.webui.utils.salt.custom;

import com.suse.salt.netapi.results.CmdResult;
import com.suse.salt.netapi.results.StateApplyResult;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Suse Uptime Tarcker Result Data from uptimetracker.requestdata
 */
public class UptimeTrackerRequestData {

    @SerializedName("cmd_|-dump-uptime-info_|-cat /etc/zypp/suse-uptime.log")
    private StateApplyResult<CmdResult> uptimeTrackerResult;


    /**
     * Gets the Uptime Tracking report
     * @return the attestation report if available
     */
    public Optional<StateApplyResult<CmdResult>> getUptimeTrackerReport() {
        return Optional.ofNullable(uptimeTrackerResult);
    }

    /**
     * Gets all info returned as Map for inserting into the report entry
     * @return returns data as Map.
     */
    public Map<String, Object> asMap() {
        Map<String, Object> out = new HashMap<>();
        getUptimeTrackeReport()
                .map(StateApplyResult::getChanges)
                .ifPresent(c -> {
                    if (c.getRetcode() == 0) {
                        out.put("dump-uptime-info", c.getStdout());
                    }
                });

        return out;
    }
}
