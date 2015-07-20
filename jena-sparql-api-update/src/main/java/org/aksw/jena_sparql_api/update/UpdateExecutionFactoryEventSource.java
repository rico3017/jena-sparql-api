package org.aksw.jena_sparql_api.update;

import java.util.HashSet;
import java.util.Set;

import org.aksw.jena_sparql_api.core.UpdateExecutionFactory;
import org.aksw.jena_sparql_api.update.DatasetListener;

import com.hp.hpl.jena.update.UpdateRequest;

/**
 * A wrapper for an update execution that supports
 * @author raven
 *
 */
public class UpdateExecutionFactoryEventSource
    implements UpdateExecutionFactory
{
    private UpdateContext updateContext;

    /**
     * Listeners only all update requests generated by this factory
     */
    private Set<DatasetListener> listeners = new HashSet<DatasetListener>();


    public UpdateExecutionFactoryEventSource(UpdateContext updateContext) {
        this.updateContext = updateContext;
    }

    @Override
    public UpdateProcessorEventSource createUpdateProcessor(UpdateRequest updateRequest) {
        UpdateProcessorEventSource result = new UpdateProcessorEventSource(this, updateRequest);
        return result;
    }

    public Set<DatasetListener> getListeners() {
        return listeners;
    }

    public UpdateContext getContext() {
        return updateContext;
    }
}
