/**
 * Copyright (c) 2015 SUSE LLC
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
package com.suse.manager.reactor.test;

import com.redhat.rhn.domain.product.test.SUSEProductTestUtils;
import com.redhat.rhn.domain.server.InstalledPackage;
import com.redhat.rhn.domain.server.MinionServer;
import com.redhat.rhn.domain.server.ServerConstants;
import com.redhat.rhn.domain.server.test.ServerFactoryTest;
import com.redhat.rhn.testing.JMockBaseTestCaseWithUser;
import com.redhat.rhn.testing.TestUtils;

import com.suse.manager.reactor.messaging.RegisterMinionEventMessageAction;
import com.suse.manager.reactor.messaging.UpdatePackageProfileEventMessage;
import com.suse.manager.reactor.messaging.UpdatePackageProfileEventMessageAction;
import com.suse.manager.webui.services.SaltService;
import com.suse.manager.webui.utils.salt.Zypper;
import com.suse.salt.netapi.calls.modules.Pkg;
import com.suse.salt.netapi.parser.JsonParser;
import org.jmock.Mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tests for {@link RegisterMinionEventMessageAction}.
 */
public class UpdatePackageProfileActionTest extends JMockBaseTestCaseWithUser {

    /**
     * Test the minion registration.
     *
     * @throws Exception
     * @throws ClassNotFoundException
     */
    public void testDoExecute() throws Exception {

        Mock saltServiceMock = mock(SaltService.class);

        MinionServer minion = (MinionServer) ServerFactoryTest.createTestServer(user, true,
                ServerConstants.getServerGroupTypeSaltEntitled(),
                ServerFactoryTest.TYPE_SERVER_MINION);

        SUSEProductTestUtils.createVendorSUSEProducts();

        final String minionId = minion.getMinionId();

        //mock out test relevant apis
        saltServiceMock.stubs().method("getInstalledPackageDetails")
                .with(eq(minionId), eq(UpdatePackageProfileEventMessageAction.PKGATTR))
                .will(returnValue(this.getMinionPackages()));

        saltServiceMock.stubs().method("getInstalledProducts")
                .with(eq(minionId))
                .will(returnValue(this.getInstalledProducts()));

        SaltService saltService = (SaltService) saltServiceMock.proxy();

        UpdatePackageProfileEventMessage message = new UpdatePackageProfileEventMessage(minion.getId());
        UpdatePackageProfileEventMessageAction action = new UpdatePackageProfileEventMessageAction(saltService);
        action.doExecute(message);

        for (InstalledPackage pkg : minion.getPackages()) {
            String release = null;
            String version = null;
            if (pkg.getName().getName().equals("aaa_base")) {
                release = "12.1";
                version = "13.2+git20140911.61c1681";
            }
            else if (pkg.getName().getName().equals("bash")) {
                release = "75.2";
                version = "4.2";
            }

            assertEquals(release, pkg.getEvr().getRelease());
            assertEquals(version, pkg.getEvr().getVersion());
            assertNull(pkg.getEvr().getEpoch());
            assertEquals("x86_64", pkg.getArch().getName());
        }
        assertEquals(2, minion.getPackages().size());


        minion.getInstalledProducts().stream().forEach(product -> {
            assertEquals("sles", product.getName());
            assertEquals("12.1", product.getVersion());
            assertEquals(null, product.getRelease());
            assertEquals("x86_64", product.getArch().getName());
        });
        assertEquals(1, minion.getInstalledProducts().size());
    }

    private Map<String, Pkg.Info> getMinionPackages() throws IOException, ClassNotFoundException {
        return new JsonParser<>(Pkg.infoInstalled(UpdatePackageProfileEventMessageAction.PKGATTR, true)
                .getReturnType()).parse(readFile("dummy_package.json"));
    }

    private List<Zypper.RealProductInfo> getInstalledProducts() throws IOException, ClassNotFoundException {
        List<Map<String, Zypper.ProductInfo>> parse = new JsonParser<>(Zypper.listProducts(false)
                .getReturnType()).parse(readFile("dummy_installed_products.json"));
        return parse.stream().flatMap(map -> map.entrySet().stream())
                .map(productEntry -> {
            Zypper.ProductInfo value = productEntry.getValue();
            return new Zypper.RealProductInfo(
                productEntry.getKey(),
                value.getArch(),
                value.getDescription(),
                value.getEol(),
                value.getEpoch(),
                value.getFlavor(),
                value.getInstalled(),
                value.getIsbase(),
                value.getProductline(),
                value.getRegisterrelease(),
                value.getRelease(),
                value.getRepo(),
                value.getShortname(),
                value.getSummary(),
                value.getVendor(),
                value.getVersion()
            );
         }).collect(Collectors.toList());
    }

    private String readFile(String file) throws IOException, ClassNotFoundException {
        return Files.lines(new File(TestUtils.findTestData(
                "/com/suse/manager/reactor/test/" + file).getPath()
        ).toPath()).collect(Collectors.joining("\n"));
    }

}
