# Copyright (c) 2021 SUSE LLC
# Licensed under the terms of the MIT license.

@scope_ansible
Feature: Operate an Ansible control node in a normal minion

  Scenario: Pre-requisite: Deploy test playbooks and inventory file
     Given I deploy testing playbooks and inventory files to "sle_minion"

  Scenario: Enable "Ansible control node" system type
     Given I am on the Systems overview page of this "sle_minion"
     When I follow "Properties" in the content area
     And I check "ansible_control_node"
     And I click on "Update Properties"
     Then I should see a "Ansible Control Node type has been applied." text

  Scenario: Apply highstate and check that Ansible is installed
     Given I am on the Systems overview page of this "sle_minion"
     When I follow "States" in the content area
     And I click on "Apply Highstate"
     And I wait until event "Apply highstate scheduled by admin" is completed
     Then "ansible" should be installed on "sle_minion"

  Scenario: The Ansible tab appears in the system overview page
     Given I am on the Systems overview page of this "sle_minion"
     When I follow "Ansible" in the content area
     Then I should see a "Ansible Control Node Configuration" text

  Scenario: Configure some inventory and playbooks path
     Given I am on the Systems overview page of this "sle_minion"
     When I follow "Ansible" in the content area
     Then I should see a "Ansible Control Node Configuration" text
     And I enter "/srv/playbooks/" as "new_playbook_path_input"
     And I click on button with id "new_playbook_path_save"
     And I enter "/srv/playbooks/example_playbook2_orion_dummy/hosts" as "new_inventory_path_input"
     And I click on button with id "new_inventory_path_save"

  Scenario: Display inventories
     Given I am on the Systems overview page of this "sle_minion"
     When I follow "Ansible" in the content area
     And I follow "Inventories" in the content area
     And I wait until I see "/srv/playbooks/example_playbook2_orion_dummy/hosts" text
     Then I click on "/srv/playbooks/example_playbook2_orion_dummy/hosts" text in Ansible paths
     And I should see a "myself" text

  Scenario: Discover playbooks and display them
     Given I am on the Systems overview page of this "sle_minion"
     When I follow "Ansible" in the content area
     And I follow "Playbooks" in the content area
     And I wait until I see "/srv/playbooks" text
     And I click on "/srv/playbooks" text in Ansible paths
     Then I wait until I see "/srv/playbooks/example_playbook2_orion_dummy/example_playbook2_orion_dummy.yml" text

  Scenario: Run a playbook using custom inventory
     Given I am on the Systems overview page of this "sle_minion"
     When I follow "Ansible" in the content area
     And I follow "Playbooks" in the content area
     And I wait until I see "/srv/playbooks" text
     And I click on "/srv/playbooks" text in Ansible paths
     And I wait until I see "/srv/playbooks/example_playbook2_orion_dummy/example_playbook2_orion_dummy.yml" text
     Then I click on "example_playbook2_orion_dummy/example_playbook2_orion_dummy.yml"
     And I wait until I see "Playbook Content" text
     And I select "/srv/playbooks/example_playbook2_orion_dummy/hosts" from "inventory-path-select"
     And I click on "Schedule"
     And I should see a "Playbook execution scheduled successfully" text
     And I wait until event "Execute playbook 'example_playbook2_orion_dummy.yml' scheduled by admin" is completed
     And file "/tmp/example_file.txt" should exist on "sle_minion"

  Scenario: Cleanup: Disable Ansible and remove test playbooks and inventory file
     Given I am on the Systems overview page of this "sle_minion"
     When I follow "Properties" in the content area
     And I uncheck "ansible_control_node"
     And I click on "Update Properties"
     Then I should see a "System properties changed" text
     And I apply highstate on "sle_minion"
     And "ansible" should be installed on "sle_minion"
     And I remove testing playbooks and inventory files from "sle_minion"
     And I remove "/tmp/example_file.txt" from "sle_minion"
