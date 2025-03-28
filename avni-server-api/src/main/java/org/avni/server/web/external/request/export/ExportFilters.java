package org.avni.server.web.external.request.export;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExportFilters {
    private List<Long> addressLevelIds;
    private DateFilter date;
    private boolean includeVoided = false;

    public List<Long> getAddressLevelIds() {
        return addressLevelIds != null ? addressLevelIds : Collections.emptyList();
    }

    public void setAddressLevelIds(List<Long> addressLevelIds) {
        this.addressLevelIds = addressLevelIds;
    }

    public DateFilter getDate() {
        return date == null ? new DateFilter() : date;
    }

    public void setDate(DateFilter date) {
        this.date = date;
    }

    public boolean includeVoided() {
        return includeVoided;
    }

    public void setIncludeVoided(boolean includeVoided) {
        this.includeVoided = includeVoided;
    }

    public static class DateFilter {
        private DateTime to;
        private DateTime from;

        public DateFilter(DateTime to, DateTime from) {
            this.to = to;
            this.from = from;
        }

        public DateFilter() {
        }

        public DateTime getTo() {
            return to;
        }

        public void setTo(DateTime to) {
            this.to = to;
        }

        public DateTime getFrom() {
            return from;
        }

        public void setFrom(DateTime from) {
            this.from = from;
        }

        public boolean apply(DateTime inputDateTime) {
            if (inputDateTime == null) {
                return false;
            }
            if (inputDateTime.isBefore(from) || inputDateTime.isAfter(to)) {
                return false;
            }
            return true;
        }
    }
}
