package org.jboss.narayana.performance.rts.client;

import io.narayana.perf.Result;
import io.narayana.perf.Worker;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;

import org.jboss.jbossts.star.util.TxLinkNames;
import org.jboss.jbossts.star.util.TxSupport;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class TestWorkerImpl implements Worker<String> {

    private static final Logger LOG = Logger.getLogger(TestWorkerImpl.class);

    private final String firstServiceUrl;

    private final String secondServiceUrl;

    private final String coordinatorUrl;

    public TestWorkerImpl(final String firstServiceUrl, final String secondServiceUrl, final String coordinatorUrl) {
        this.firstServiceUrl = firstServiceUrl;
        this.secondServiceUrl = secondServiceUrl;
        this.coordinatorUrl = coordinatorUrl;
    }

    public String doWork(String context, int iterationsCount, Result<String> options) {
        for (int i = 0; i < iterationsCount; i++) {
            executeIteration();
        }

        return null;
    }

    public void init() {

    }

    public void fini() {

    }

    private void executeIteration() {
        // Cannot use single instance per object, since it is not thread-safe.
        final TxSupport txSupport = new TxSupport(coordinatorUrl);

        try {
            txSupport.startTx();
            invokeService(firstServiceUrl, txSupport);
            invokeService(secondServiceUrl, txSupport);
            txSupport.commitTx();
        } catch (final Throwable t) {
            LOG.warnv(t, "Failure during one of the executions. Rolling back the transaction: ", txSupport.getTxnUri());
            txSupport.rollbackTx();
        }
    }

    private void invokeService(final String serviceUrl, final TxSupport txSupport) throws Exception {
        final Client client = ClientBuilder.newClient();
        final Link participantLink = Link.fromUri(txSupport.getTxnUri()).rel(TxLinkNames.PARTICIPANT)
                .title(TxLinkNames.PARTICIPANT).build();

        final Response response = client.target(serviceUrl).request().header("link", participantLink).post(null);

        if (response.getStatus() != 204) {
            throw new Exception("Unexpected response code recieved from the service: " + serviceUrl + ". "
                    + "Response code: " + response.getStatus());
        }
    }

}