# Copyright (c) 2017 SUSE LLC
# Licensed under the terms of the MIT license.

Feature: Test action chaining

  Scenario: wait for taskomatic finished required jobs
    Given Patches are visible for the registered client

  Scenario: I add a package installation to an action chain
    Given I am on the Systems overview page of this "sle-client"
    When I follow "Software" in the content area
    And I follow "Install New Packages" in the content area
    And I check "hoag-dummy-1.1-2.1" in the list
    And I click on "Install Selected Packages"
    And I check radio button "schedule-by-action-chain"
    And I click on "Confirm"
    Then I should see a "Action has been successfully added to the Action Chain" text

  Scenario: I add a remote command to the action chain
    Given I am on the Systems overview page of this "sle-client"
    When I follow "Remote Command"
    And I enter as remote command this script in
      """
      #!/bin/bash
      touch /root/12345
      """
    And I check radio button "schedule-by-action-chain"
    And I click on "Schedule"
    Then I should see a "Action has been successfully added to the Action Chain" text

  Scenario: I add a patch installation to the action chain
    Given I am on the Systems overview page of this "sle-client"
    When I follow "Software" in the content area
    And I follow "Patches" in the content area
    And I check "andromeda-dummy-6789" in the list
    And I click on "Apply Patches"
    And I check radio button "schedule-by-action-chain"
    And I click on "Confirm"
    Then I should see a "Action has been successfully added to the Action Chain" text

  Scenario: I add a remove package to the action chain
    Given I am on the Systems overview page of this "sle-client"
    When I follow "Software" in the content area
    And I follow "List / Remove" in the content area
    And I check "adaptec-firmware" in the list
    And I click on "Remove Packages"
    And I check radio button "schedule-by-action-chain"
    And I click on "Confirm"
    Then I should see a "Action has been successfully added to the Action Chain" text

  Scenario: I add a verify package to the action chain
    Given I am on the Systems overview page of this "sle-client"
    When I follow "Software" in the content area
    And I follow "Verify" in the content area
    And I check "andromeda-dummy-1.0-4.1" in the list
    And I click on "Verify Selected Packages"
    And I check radio button "schedule-by-action-chain"
    And I click on "Confirm"
    Then I should see a "Action has been successfully added to the Action Chain" text

  Scenario: I add a reboot action to the action chain
    Given I am on the Systems overview page of this "sle-client"
    When I follow "Schedule System Reboot" in the content area
    And I check radio button "schedule-by-action-chain"
    And I click on "Reboot system"
    Then I should see a "Action has been successfully added to the Action Chain" text

  Scenario: I verify the action chain list
    Given I am on the Systems overview page of this "sle-client"
    When I follow "Schedule"
    And I follow "Action Chains"
    And I follow "new action chain"
    And I should see a "1. Install or update hoag-dummy on 1 system" text
    And I should see a "2. Run a remote command on 1 system" text
    And I should see a "3. Apply patch(es) andromeda-dummy-6789 on 1 system" text
    And I should see a "4. Remove adaptec-firmware from 1 system" text
    And I should see a "5. Verify andromeda-dummy on 1 system" text
    Then I should see a "6. Reboot 1 system" text

  Scenario: check that different user cannot see the action chain
    Given I am authorized as "testing" with password "testing"
    When I follow "Schedule"
    And I follow "Action Chains"
    Then I should not see a "new action chain" link

  Scenario: I delete the action chain
     Given I am authorized as "admin" with password "admin"
     Then I follow "Schedule"
     And I follow "Action Chains"
     And I follow "new action chain"
     And I follow "delete action chain" in the content area
     Then I click on "Delete"

  Scenario: I add a remote command to new action chain
    Given I am on the Systems overview page of this "sle-client"
    When I follow "Remote Command"
    And I enter as remote command this script in
      """
      #!/bin/bash
      touch /root/webui-actionchain-test
      """
    And I check radio button "schedule-by-action-chain"
    And I click on "Schedule"
    Then I should see a "Action has been successfully added to the Action Chain" text

  Scenario: I execute the action chain from the web ui
    Given I am on the Systems overview page of this "sle-client"
    When I follow "Schedule"
    And I follow "Action Chains"
    And I follow "new action chain"
    And I should see a "1. Run a remote command on 1 system" text
    Then I click on "Save and Schedule"
    And I should see a "Action Chain new action chain has been scheduled for execution." text
    When I run rhn_check on this client
    Then "/root/webui-actionchain-test" exists on the filesystem of "sle-client"
    Then I run "rm /root/webui-actionchain-test" on "sle-client"

  Scenario: Basic chain operations xmlrpc
    Given I am logged in via XML-RPC/actionchain as user "admin" and password "admin"
    When I call XML-RPC/createChain with chainLabel "Quick Brown Fox"
    And I call actionchain.listChains() if label "Quick Brown Fox" is there
    Then I delete the action chain
    And there should be no action chain with the label "Quick Brown Fox"
    When I call XML-RPC/createChain with chainLabel "Quick Brown Fox"
    Then I call actionchain.renameChain() to rename it from "Quick Brown Fox" to "Slow Gray Elephant"
    And there should be a new action chain with the label "Slow Gray Elephant"
    And I delete an action chain, labeled "Slow Gray Elephant"
    And there should be no action chain with the label "Slow Gray Elephant"
    And no action chain with the label "Quick Brown Fox".

  Scenario: Schedule operations via xmlrpc
    Given I am logged in via XML-RPC/actionchain as user "admin" and password "admin"
    When I call XML-RPC/createChain with chainLabel "Quick Brown Fox"
    And I call actionchain.addPackageInstall()
    And I call actionchain.addPackageRemoval()
    And I call actionchain.addPackageUpgrade()
    And I call actionchain.addPackageVerify()
    And I call actionchain.addScriptRun() with the script like "#!/bin/bash\nexit 1;"
    And I call actionchain.addSystemReboot()
    Then I should be able to see all these actions in the action chain
    When I call actionchain.removeAction on each action within the chain
    Then I should be able to see that the current action chain is empty
    And I delete the action chain

  Scenario: Run the action chain via xmlrpc
    Given I am logged in via XML-RPC/actionchain as user "admin" and password "admin"
    When I call XML-RPC/createChain with chainLabel "Quick Brown Fox"
    And I call actionchain.addSystemReboot()
    Then I should be able to see all these actions in the action chain
    When I schedule the action chain
    Then there should be no more my action chain
    And I should see scheduled action, called "System reboot scheduled by admin"
    Then I cancel all scheduled actions
    And there should be no more any scheduled actions
