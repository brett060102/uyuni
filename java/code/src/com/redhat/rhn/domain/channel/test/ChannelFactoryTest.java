/**
 * Copyright (c) 2009--2017 Red Hat, Inc.
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
package com.redhat.rhn.domain.channel.test;

import com.redhat.rhn.common.hibernate.HibernateFactory;
import com.redhat.rhn.domain.channel.Channel;
import com.redhat.rhn.domain.channel.ChannelArch;
import com.redhat.rhn.domain.channel.ChannelFactory;
import com.redhat.rhn.domain.channel.ChannelFamily;
import com.redhat.rhn.domain.channel.ChannelFamilyFactory;
import com.redhat.rhn.domain.channel.ClonedChannel;
import com.redhat.rhn.domain.channel.ProductName;
import com.redhat.rhn.domain.common.ChecksumType;
import com.redhat.rhn.domain.kickstart.KickstartInstallType;
import com.redhat.rhn.domain.kickstart.test.KickstartDataTest;
import com.redhat.rhn.domain.kickstart.test.KickstartableTreeTest;
import com.redhat.rhn.domain.org.Org;
import com.redhat.rhn.domain.rhnpackage.Package;
import com.redhat.rhn.domain.rhnpackage.test.PackageTest;
import com.redhat.rhn.domain.user.User;
import com.redhat.rhn.manager.channel.ChannelManager;
import com.redhat.rhn.manager.rhnpackage.test.PackageManagerTest;
import com.redhat.rhn.manager.user.UserManager;
import com.redhat.rhn.testing.ChannelTestUtils;
import com.redhat.rhn.testing.RhnBaseTestCase;
import com.redhat.rhn.testing.TestUtils;
import com.redhat.rhn.testing.UserTestUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * ChannelFactoryTest
 * @version $Rev$
 */
public class ChannelFactoryTest extends RhnBaseTestCase {

    public void testChannelFactory() throws Exception {
        User user = UserTestUtils.findNewUser("testUser",
                "testChannelFactory" + this.getClass().getSimpleName());
        Channel c = ChannelFactoryTest.createTestChannel(user);

        assertNotNull(c.getChannelFamily());

        Channel c2 = ChannelFactory.lookupById(c.getId());
        assertEquals(c.getLabel(), c2.getLabel());

        Channel c3 = ChannelFactoryTest.createTestChannel(user);
        Long id = c3.getId();
        assertNotNull(c.getChannelArch());
        ChannelFactory.remove(c3);
        flushAndEvict(c3);
        assertNull(ChannelFactory.lookupById(id));
    }

    public static ProductName lookupOrCreateProductName(String label) throws Exception {
        ProductName attempt = ChannelFactory.lookupProductNameByLabel(label);
        if (attempt == null) {
            attempt = new ProductName();
            attempt.setLabel(label);
            attempt.setName(label);
            HibernateFactory.getSession().save(attempt);
        }
        return attempt;
    }

    public static Channel createBaseChannel(User user) throws Exception {
        Channel c = ChannelFactoryTest.createTestChannel(user);
        c.setOrg(user.getOrg());

        ProductName pn = lookupOrCreateProductName(ChannelManager.RHEL_PRODUCT_NAME);
        c.setProductName(pn);

        ChannelFactory.save(c);
        return c;
    }

    public static Channel createBaseChannel(User user, String channelArchLabel) throws Exception {
        Channel c = ChannelFactoryTest.createTestChannel(user, channelArchLabel);
        c.setOrg(user.getOrg());

        ProductName pn = lookupOrCreateProductName(ChannelManager.RHEL_PRODUCT_NAME);
        c.setProductName(pn);

        ChannelFactory.save(c);
        return c;
    }

    public static Channel createBaseChannel(User user,
                                ChannelFamily fam) throws Exception {
        Channel c = createTestChannel(null, fam);
        ProductName pn = lookupOrCreateProductName(ChannelManager.RHEL_PRODUCT_NAME);
        c.setProductName(pn);
        ChannelFactory.save(c);
        return (Channel)TestUtils.saveAndReload(c);
    }

    public static Channel createTestChannel(User user) throws Exception {
        Channel c = ChannelFactoryTest.createTestChannel(user.getOrg());
        // assume we want the user to have access to this channel once created
        UserManager.addChannelPerm(user, c.getId(), "subscribe");
        UserManager.addChannelPerm(user, c.getId(), "manage");
        ChannelFactory.save(c);
        return c;
    }

    public static Channel createTestChannel(User user, String channelLabel) throws Exception {
        String query = "ChannelArch.findByLabel";
        ChannelArch arch = (ChannelArch) TestUtils.lookupFromCacheByLabel(channelLabel, query);
        Channel c = createTestChannel(user.getOrg(), arch, user.getOrg().getPrivateChannelFamily());
        // assume we want the user to have access to this channel once created
        UserManager.addChannelPerm(user, c.getId(), "subscribe");
        UserManager.addChannelPerm(user, c.getId(), "manage");
        ChannelFactory.save(c);
        return c;
    }

    public static Channel createTestChannel(Org org) throws Exception {
        ChannelFamily cfam = org.getPrivateChannelFamily();
        Channel c =  ChannelFactoryTest.createTestChannel(org, cfam);
        ChannelFactory.save(c);
        return c;
    }

    public static Channel createTestChannel(Org org, ChannelFamily cfam) throws Exception {
        String query = "ChannelArch.findById";
        ChannelArch arch = (ChannelArch) TestUtils.lookupFromCacheById(500L, query);
        return createTestChannel(org, arch, cfam);
    }

    /**
     * Create a test channel setting the GPGCheck flag via a parameter.
     *
     * @param user the user
     * @param gpgCheckIn the GPGCheck flag to set
     * @return the test channel
     * @throws Exception
     */
    public static Channel createTestChannel(User user, boolean gpgCheckIn) throws Exception {
        Channel c = ChannelFactoryTest.createTestChannel(user.getOrg());
        c.setGPGCheck(gpgCheckIn);
        // assume we want the user to have access to this channel once created
        UserManager.addChannelPerm(user, c.getId(), "subscribe");
        UserManager.addChannelPerm(user, c.getId(), "manage");
        ChannelFactory.save(c);
        return c;
    }

    public static Channel createTestChannel(Org org, ChannelArch arch, ChannelFamily cfam) throws Exception {
        String label = "channellabel" + TestUtils.randomString().toLowerCase();
        String basedir = "TestChannel basedir";
        String name = "ChannelName" + TestUtils.randomString();
        String summary = "TestChannel summary";
        String description = "TestChannel description";
        Date lastmodified = new Date();
        Date created = new Date();
        Date modified = new Date();
        String gpgurl = "https://gpg.url";
        String gpgid = "B3BCE11D";
        String gpgfp = "AAAA BBBB CCCC DDDD EEEE FFFF 7777 8888 9999 0000";
        Calendar cal = Calendar.getInstance();
        cal.roll(Calendar.DATE, true);
        Date endoflife = new Date(System.currentTimeMillis() + Integer.MAX_VALUE);

        Channel c = new Channel();
        c.setOrg(org);
        c.setLabel(label);
        c.setBaseDir(basedir);
        c.setName(name);
        c.setSummary(summary);
        c.setDescription(description);
        c.setLastModified(lastmodified);
        c.setCreated(created);
        c.setModified(modified);
        c.setGPGKeyUrl(gpgurl);
        c.setGPGKeyId(gpgid);
        c.setGPGKeyFp(gpgfp);
        c.setEndOfLife(endoflife);
        c.setChannelArch(arch);
        c.setChannelFamily(cfam);
        ChannelFactory.save(c);
        return c;
    }

    /**
     * TODO: need to fix this test when we put errata management back in.
     * @throws Exception something bad happened
     */
    public void testChannelsWithClonableErrata() throws Exception {
        User user = UserTestUtils.findNewUser("testUser",
                "testOrg" + this.getClass().getSimpleName());
        ChannelManager.
            getChannelsWithClonableErrata(user.getOrg());

        Channel original = ChannelFactoryTest.createTestChannel(user);
        Channel clone = ChannelFactoryTest.createTestClonedChannel(original, user);
        TestUtils.flushAndEvict(original);
        TestUtils.flushAndEvict(clone);

        List<ClonedChannel> channels =
                ChannelFactory.getChannelsWithClonableErrata(
                user.getOrg());

        assertTrue(channels.size() > 0);
    }

    public void testLookupByLabel() throws Exception {
        User user = UserTestUtils.findNewUser("testuser", "testorg");
        Channel rh = createTestChannel(user);
        String label = rh.getLabel();
        rh.setOrg(null);
        ChannelFactory.save(rh);
        assertNull(rh.getOrg());

        //Lookup a channel without an org (An RH channel)
        Channel c = ChannelFactory.lookupByLabel(user.getOrg(), label);
        assertEquals(label, c.getLabel());

        //Lookup a channel with an org (user custom channel)
        Channel cust = createTestChannel(user);
        label = cust.getLabel();
        assertNotNull(cust.getOrg());
        c = ChannelFactory.lookupByLabel(user.getOrg(), label);
        assertNotNull(c);
        assertEquals(label, c.getLabel());
        assertEquals(user.getOrg(), c.getOrg());

        //Lookup a channel in a different org
    }

    public void testIsGloballySubscribable() throws Exception {
        User user = UserTestUtils.findNewUser("testUser",
                "testOrg" + this.getClass().getSimpleName());
        Channel c = createTestChannel(user);
        assertTrue(ChannelFactory.isGloballySubscribable(user.getOrg(), c));
    }

    public void testChannelArchByLabel() {
        assertNull("Arch found for null label",
                ChannelFactory.findArchByLabel(null));
        assertNull("Arch found for invalid label",
                ChannelFactory.findArchByLabel("some-invalid_arch_label"));

        ChannelArch ca = ChannelFactory.findArchByLabel("channel-x86_64");
        assertNotNull(ca);
        assertEquals("channel-x86_64", ca.getLabel());
        assertEquals("x86_64", ca.getName());
    }

    public void testVerifyLabel() throws Exception {
        User user = UserTestUtils.findNewUser("testUser",
                "testOrg" + this.getClass().getSimpleName());
        Channel c = createTestChannel(user);
        assertFalse(ChannelFactory.doesChannelLabelExist("foo"));
        assertTrue(ChannelFactory.doesChannelLabelExist(c.getLabel()));
    }

    public void testVerifyName() throws Exception {
        User user = UserTestUtils.findNewUser("testUser",
                "testOrg" + this.getClass().getSimpleName());
        Channel c = createTestChannel(user);
        assertFalse(ChannelFactory.doesChannelNameExist("power house foo channel"));
        assertTrue(ChannelFactory.doesChannelNameExist(c.getName()));
    }

    public void testKickstartableTreeChannels() throws Exception {
        User user = UserTestUtils.findNewUser("testUser",
                "testOrg" + this.getClass().getSimpleName());

        List<Channel> channels = ChannelFactory.getKickstartableTreeChannels(user.getOrg());
        assertNotNull(channels);
        int originalSize = channels.size();

        createTestChannel(user);

        channels = ChannelFactory.getKickstartableTreeChannels(user.getOrg());
        assertEquals(originalSize + 1, channels.size());
    }

    public void testKickstartableChannels() throws Exception {
        User user = UserTestUtils.findNewUser("testUser",
                "testOrg" + this.getClass().getSimpleName());
        // Setup test config since kickstartable trees are required
        KickstartDataTest.setupTestConfiguration(user);

        List<Channel> channels = ChannelFactory.getKickstartableChannels(user.getOrg());
        assertNotNull(channels);
        int originalSize = channels.size();

        // c1 is kickstartable
        Channel c1 = createTestChannel(user);
        KickstartableTreeTest.createTestKickstartableTree(c1,
                KickstartInstallType.RHEL_7);
        KickstartableTreeTest.createTestKickstartableTree(c1,
                KickstartInstallType.RHEL_7);
        // c2 is kickstartable
        Channel c2 = createTestChannel(user);
        KickstartableTreeTest.createTestKickstartableTree(c2,
                KickstartInstallType.FEDORA_PREFIX + "18");
        // c3 is not kickstartable
        Channel c3 = createTestChannel(user);
        KickstartableTreeTest.createTestKickstartableTree(c3,
                KickstartInstallType.SLES_PREFIX + "11generic");
        // c4 is not kickstartable
        createTestChannel(user);

        channels = ChannelFactory.getKickstartableChannels(user.getOrg());
        assertEquals(originalSize + 2, channels.size());
        assertTrue(channels.contains(c1));
        assertTrue(channels.contains(c2));
    }

    public void testPackageCount() throws Exception {
        User user = UserTestUtils.findNewUser("testUser",
                "testOrg" + this.getClass().getSimpleName());
        Channel original = ChannelFactoryTest.createTestChannel(user);
        assertEquals(0, ChannelFactory.getPackageCount(original));
        original.addPackage(PackageTest.createTestPackage(user.getOrg()));
        ChannelFactory.save(original);
        TestUtils.flushAndEvict(original);

        original = (Channel)reload(original);
        assertEquals(1, ChannelFactory.getPackageCount(original));
    }

    /**
     * Create a test cloned channel. NOTE: This function does not copy its
     * original's package list like a real clone would. It is only useful for
     * testing purposes.
     * @param original Channel to be cloned
     * @param user the user
     * @return a test cloned channel
     */
    public static Channel createTestClonedChannel(Channel original, User user) {
        Org org = user.getOrg();
        ClonedChannel clone = new ClonedChannel();
        ChannelFamily cfam = ChannelFamilyFactory.lookupOrCreatePrivateFamily(org);

        clone.setOrg(org);
        clone.setLabel("clone-" + original.getLabel());
        clone.setBaseDir(original.getBaseDir());
        clone.setName("Clone of " + original.getName());
        clone.setSummary(original.getSummary());
        clone.setDescription(original.getDescription());
        clone.setLastModified(new Date());
        clone.setCreated(new Date());
        clone.setModified(new Date());
        clone.setGPGKeyUrl(original.getGPGKeyUrl());
        clone.setGPGKeyId(original.getGPGKeyId());
        clone.setGPGKeyFp(original.getGPGKeyFp());
        clone.setGPGCheck(original.isGPGCheck());
        clone.setEndOfLife(new Date());
        clone.setChannelFamily(cfam);
        clone.setChannelArch(original.getChannelArch());

        /* clone specific calls */
        clone.setOriginal(original);

        ChannelFactory.save(clone);

        // assume we want the user to have access to this channel once created
        UserManager.addChannelPerm(user, clone.getId(), "subscribe");
        UserManager.addChannelPerm(user, clone.getId(), "manage");

        return clone;
    }
    public void testAccessibleChildChannels() throws Exception {
        User user = UserTestUtils.findNewUser("testUser",
                "testOrg" + this.getClass().getSimpleName());
        Channel parent = ChannelFactoryTest.createBaseChannel(user);
        Channel child = ChannelFactoryTest.createTestChannel(user);
        child.setParentChannel(parent);
        TestUtils.saveAndFlush(child);
        TestUtils.saveAndFlush(parent);
        TestUtils.flushAndEvict(child);
        List<Channel> dr = parent.getAccessibleChildrenFor(user);

        assertFalse(dr.isEmpty());
        assertEquals(child, dr.get(0));
    }

    public static ProductName createProductName() {
        ProductName pn = new ProductName();
        pn.setLabel("Label - " + TestUtils.randomString());
        pn.setName("Name - " + TestUtils.randomString());
        TestUtils.saveAndFlush(pn);
        return pn;
    }

    public void testFindChannelArchesSyncdChannels() throws Exception {
        // ensure at least one channel is present
        User user = UserTestUtils.findNewUser("testuser", "testorg");
        ChannelFactoryTest.createTestChannel(user);

        List<String> labels = ChannelFactory.findChannelArchLabelsSyncdChannels();
        assertNotNull(labels);
        assertNotEmpty(labels);
    }

    public void testListAllBaseChannels() throws Exception {
        User user = UserTestUtils.findNewUser("testUser",
                "testOrg" + this.getClass().getSimpleName());
        // do NOT use createBaseChannel here because that will create a Red Hat
        // base channel NOT a user owned base channel.
        createTestChannel(user);
        List<Channel> channels = ChannelFactory.listAllBaseChannels(user);
        assertNotNull(channels);
        int size = channels.size();
        createTestChannel(user);
        channels = ChannelFactory.listAllBaseChannels(user);
        assertNotNull(channels);
        assertEquals(size + 1, channels.size());
    }

    public void testLookupPackageByFileName() throws Exception {
        User user = UserTestUtils.findNewUser("testUser",
                "testOrg" + this.getClass().getSimpleName());
        Channel channel = ChannelTestUtils.createTestChannel(user);
        TestUtils.saveAndFlush(channel);
        Package p = PackageManagerTest.addPackageToChannel("some-package", channel);
        String fileName = "some-package-2.13.1-6.fc9.x86_64.rpm";
        p.setPath("redhat/1/c7d/some-package/2.13.1-6.fc9/" +
                "x86_64/c7dd5e9b6975bc7f80f2f4657260af53/" +
                fileName);
        TestUtils.saveAndFlush(p);

        Package lookedUp = ChannelFactory.lookupPackageByFilename(channel,
                fileName);
        assertNotNull(lookedUp);
        assertEquals(p.getId(), lookedUp.getId());

        // Test in child channel.
        Channel child = ChannelTestUtils.createChildChannel(user, channel);
        Package cp = PackageManagerTest.addPackageToChannel("some-package-child", child);
        String fileNameChild = "some-package-child-2.13.1-6.fc9.x86_64.rpm";
        cp.setPath("redhat/1/c7d/some-package-child/2.13.1-6.fc9/" +
                "x86_64/c7dd5e9b6975bc7f80f2f4657260af53/" +
                fileNameChild);

        Package lookedUpChild = ChannelFactory.lookupPackageByFilename(channel,
                fileNameChild);
        assertNotNull(lookedUpChild);
        assertEquals(cp.getId(), lookedUpChild.getId());

    }

    public void testfindChecksumByLabel() {
        assertNull("Checksum found for null label",
                ChannelFactory.findChecksumTypeByLabel(null));
        assertNull("Checksum found for invalid label",
                ChannelFactory.findChecksumTypeByLabel("some-invalid_checksum"));

        ChecksumType ct = ChannelFactory.findChecksumTypeByLabel("sha256");
        assertNotNull(ct);
        assertEquals("sha256", ct.getLabel());

        ChecksumType ct2 = ChannelFactory.findChecksumTypeByLabel("sha1");
        assertNotNull(ct2);
        assertEquals("sha1", ct2.getLabel());
    }

    /**
     * Test user channel accessibility
     *
     * @throws Exception if anything goes wrong
     */
    public void testAccessibility() throws Exception {
        User user1 = UserTestUtils.findNewUser("testuser1", "testorg1");
        User user2 = UserTestUtils.createUser("testuser2", user1.getOrg().getId());
        User user3 = UserTestUtils.findNewUser("testuser3", "testorg3");
        Channel c = ChannelFactoryTest.createTestChannel(user1);

        assertTrue(ChannelFactory.isAccessibleByUser(c.getLabel(), user1.getId()));
        assertTrue(ChannelFactory.isAccessibleByUser(c.getLabel(), user2.getId()));
        assertFalse(ChannelFactory.isAccessibleByUser(c.getLabel(), user3.getId()));
    }
}
