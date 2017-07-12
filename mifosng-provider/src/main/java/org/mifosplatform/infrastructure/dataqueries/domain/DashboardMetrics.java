/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataqueries.domain;

import org.mifosplatform.organisation.office.domain.Office;
import org.mifosplatform.organisation.staff.domain.Staff;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.joda.time.LocalDateTime;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "m_dashboard_metric_result")
public class DashboardMetrics extends AbstractPersistable<Long> {

    @Column(name="run_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date runDate;

    @Column(name = "metric_value", scale = 6, precision = 19)
    private BigDecimal metricValue;

    @Column(name = "metric_name", nullable = false)
    private String metricName;


    @Column(name = "office_id", nullable = false)
    private Long officeId;


    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "month_year", nullable = false)
    private String monthYear;




    public DashboardMetrics() {
    }

    public DashboardMetrics(final BigDecimal metricValue,final String metricName,
                            final Long officeId, final Long staffId, final String monthYear ) {
        this.metricValue = metricValue;
        this.metricName = metricName;
        this.runDate = LocalDateTime.now().toDate();
        this.officeId = officeId;
        this.staffId = staffId;
        this.monthYear = monthYear;
    }

    public Date getRunDate() {
        return runDate;
    }

    public BigDecimal getMetricValue() {
        return metricValue;
    }

    public Long getOfficeId() {
        return officeId;
    }

    public Long getStaffId() {
        return staffId;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricValue(BigDecimal metricValue) {
        this.metricValue = metricValue;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getMonthYear() {
        return monthYear;
    }

    public void setMonthYear(String monthYear) {
        this.monthYear = monthYear;
    }
}
