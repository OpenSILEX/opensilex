//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

/**
 * Self diagnostics of the access log writer. A monitoring feature that silently drops what it is meant to record would be worse than none.
 *
 * <p>A bean and not a record: the swagger generator introspects properties by bean
 * convention, and a record's accessors would produce an empty model and a broken
 * TypeScript client.</p>
 *
 * @author Arnaud Charleroy
 */
public class RequestLogStatusDTO {

    @ApiModelProperty(value = "whether the writer thread is alive")
    @JsonProperty("running")
    private boolean running;

    @ApiModelProperty(value = "whether repeated MongoDB failures have paused it")
    @JsonProperty("paused")
    private boolean paused;

    @ApiModelProperty(value = "entries waiting to be written")
    @JsonProperty("queued")
    private int queued;

    @ApiModelProperty(value = "size of the buffer")
    @JsonProperty("queue_capacity")
    private int queueCapacity;

    @ApiModelProperty(value = "entries written since startup")
    @JsonProperty("written")
    private long written;

    @ApiModelProperty(value = "entries dropped since startup")
    @JsonProperty("dropped")
    private long dropped;

    @ApiModelProperty(value = "batches that could not be written")
    @JsonProperty("failed_batches")
    private long failedBatches;

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public int getQueued() {
        return queued;
    }

    public void setQueued(int queued) {
        this.queued = queued;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public long getWritten() {
        return written;
    }

    public void setWritten(long written) {
        this.written = written;
    }

    public long getDropped() {
        return dropped;
    }

    public void setDropped(long dropped) {
        this.dropped = dropped;
    }

    public long getFailedBatches() {
        return failedBatches;
    }

    public void setFailedBatches(long failedBatches) {
        this.failedBatches = failedBatches;
    }

}
