/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.accounting.closure.storeglaccountbalance.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.mifosplatform.accounting.closure.data.GLClosureData;
import org.mifosplatform.accounting.closure.service.GLClosureReadPlatformService;
import org.mifosplatform.accounting.closure.storeglaccountbalance.data.GLClosureAccountBalanceReportData;
import org.mifosplatform.accounting.closure.storeglaccountbalance.data.GLClosureJournalEntryBalanceValidator;
import org.mifosplatform.accounting.closure.storeglaccountbalance.data.GLClosureJournalEntryData;
import org.mifosplatform.accounting.closure.storeglaccountbalance.helper.UriQueryParameterHelper;
import org.mifosplatform.infrastructure.core.domain.JdbcSupport;
import org.mifosplatform.infrastructure.core.service.RoutingDataSource;
import org.mifosplatform.infrastructure.documentmanagement.contentrepository.FileSystemContentRepository;
import org.mifosplatform.organisation.office.service.OfficeReadPlatformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.csvreader.CsvWriter;

@Service
public class GLClosureJournalEntryBalanceReadPlatformServiceImpl implements GLClosureJournalEntryBalanceReadPlatformService {
    
    private final JdbcTemplate jdbcTemplate;
    private final DateTimeFormatter mysqlDateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
    private final DateTimeFormatter fileOutputDateFormatter = DateTimeFormat.forPattern("MM/dd/yyyy");
    private final GLClosureJournalEntryBalanceValidator glClosureJournalEntryBalanceValidator;
    private final OfficeReadPlatformService officeReadPlatformService;
    private final GLClosureReadPlatformService glClosureReadPlatformService;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final static Logger logger = LoggerFactory.getLogger(GLClosureJournalEntryBalanceReadPlatformServiceImpl.class);

    /**
     * @param jdbcTemplate
     */
    @Autowired
    public GLClosureJournalEntryBalanceReadPlatformServiceImpl(final RoutingDataSource routingDataSource, 
            final GLClosureJournalEntryBalanceValidator glClosureJournalEntryBalanceValidator, 
            final OfficeReadPlatformService officeReadPlatformService, 
            final GLClosureReadPlatformService glClosureReadPlatformService) {
        this.jdbcTemplate = new JdbcTemplate(routingDataSource);
        this.glClosureJournalEntryBalanceValidator = glClosureJournalEntryBalanceValidator;
        this.officeReadPlatformService = officeReadPlatformService;
        this.glClosureReadPlatformService = glClosureReadPlatformService;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(routingDataSource);
    }
    
    public static final class GLClosureJournalEntryDataMapper implements RowMapper<GLClosureJournalEntryData> {
        
        /**
         * SQL statement for retrieving the journal entries
         * @return SQL statement string
         */
        public String sql() {
            return "je.id, je.account_id, je.office_id, je.entry_date, je.created_date, je.amount, "
                    + "je.organization_running_balance, je.office_running_balance, je.description "
                    + "from acc_gl_account as ac "
                    + "inner join ( select * from (select * from acc_gl_journal_entry "
                    + "where office_id = ? and entry_date <= ? "
                    + "and is_running_balance_calculated = 1 "
                    + "order by entry_date desc, created_date desc, id desc ) t group by t.account_id ) "
                    + "as je on je.account_id = ac.id "
                    + "group by je.account_id, je.office_id "
                    + "order by entry_date, created_date ";
        }

        @Override
        public GLClosureJournalEntryData mapRow(ResultSet rs, int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final Long accountId = rs.getLong("account_id");
            final Long officeId = rs.getLong("office_id");
            final LocalDate entryDate = JdbcSupport.getLocalDate(rs, "entry_date");
            final DateTime createdDateTime = JdbcSupport.getDateTime(rs, "created_date");
            final BigDecimal amount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "amount");
            final BigDecimal organisationRunningBalance = 
                    JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "organization_running_balance");
            final BigDecimal officeRunningBalance = 
                    JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "office_running_balance");
            final String description = rs.getString("description");
            
            LocalDateTime createdDate = null;
            
            if (createdDateTime != null) {
                createdDate = new LocalDateTime(createdDateTime);
            }
            
            return GLClosureJournalEntryData.instance(id, accountId, officeId, entryDate, 
                    createdDate, amount, organisationRunningBalance, officeRunningBalance, description);
        }
    }

    @Override
    public Collection<GLClosureJournalEntryData> retrieveAllJournalEntries(Long officeId, LocalDate maxEntryDate) {
        final GLClosureJournalEntryDataMapper mapper = new GLClosureJournalEntryDataMapper();
        final String sql = "select " + mapper.sql();
        
        return this.jdbcTemplate.query(sql, mapper, new Object[] {officeId, this.mysqlDateFormatter.print(maxEntryDate)});
    }
    
    public static final class GLClosureAccountBalanceReportMapper implements RowMapper<GLClosureAccountBalanceReportData> {
        private final GLClosureData endClosure;
        private final GLClosureData startClosure;
        private final String reference;
        
        /**
         * SQL statement that will calculate GL closure account balance
         * @return SQL statement string
         */
        public String sql() {
            String sql = "sc.account_id, sc.account_number, (ec.amount - sc.amount) as amount "
                    + "from "
                    + "(select je.account_id as account_id, ga.gl_code as account_number, sum(amount) as amount "
                    + "from acc_gl_closure_journal_entry_balance je "
                    + "inner join acc_gl_closure gc "
                    + "on gc.id = je.closure_id "
                    + "inner join acc_gl_account ga "
                    + "on ga.id = je.account_id "
                    + "where gc.office_id in "
                    + "(:officeIds) " 
                    + "and gc.closing_date = :startClosureClosingDate "
                    + "and je.is_deleted = 0 "
                    + "group by je.account_id) sc "
                    + "inner join "
                    + "(select je.account_id as account_id, ga.gl_code as account_number, sum(amount) as amount "
                    + "from acc_gl_closure_journal_entry_balance je "
                    + "inner join acc_gl_closure gc "
                    + "on gc.id = je.closure_id "
                    + "inner join acc_gl_account ga "
                    + "on ga.id = je.account_id "
                    + "where gc.office_id in "
                    + "(:officeIds) " 
                    + "and gc.closing_date = :endClosureClosingDate "
                    + "and je.is_deleted = 0 "
                    + "group by je.account_id) ec "
                    + "on sc.account_id = ec.account_id "
                    + "order by account_number ";
            
            if (this.startClosure == null) {
                sql = "je.account_id as account_id, ga.gl_code as account_number, sum(amount) as amount "
                    + "from acc_gl_closure_journal_entry_balance je "
                    + "inner join acc_gl_closure gc "
                    + "on gc.id = je.closure_id "
                    + "inner join acc_gl_account ga "
                    + "on ga.id = je.account_id "
                    + "where gc.office_id in "
                    + "(:officeIds) "
                    + "and gc.closing_date = :endClosureClosingDate "
                    + "and je.is_deleted = 0 "
                    + "group by je.account_id "
                    + "order by account_number ";
            }
            
            return sql;
        }
        
        /**
         * @param endClosure
         * @param startClosure
         */
        public GLClosureAccountBalanceReportMapper(final GLClosureData endClosure,
                final GLClosureData startClosure, final String reference) {
            this.endClosure = endClosure;
            this.startClosure = startClosure;
            this.reference = reference;
        }

        @Override
        public GLClosureAccountBalanceReportData mapRow(ResultSet rs, int rowNum) throws SQLException {
            final String accountNumber = rs.getString("account_number");
            final LocalDate postedDate = new LocalDate();
            final BigDecimal amount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "amount");
            
            LocalDate transactionDate = null;
            
            if (this.endClosure != null) {
                transactionDate = this.endClosure.getClosingDate();
            }
            
            return GLClosureAccountBalanceReportData.instance(accountNumber, transactionDate, postedDate, amount, 
                    this.reference);
        }
    }

    @Override
    public File generateGLClosureAccountBalanceReport(MultivaluedMap<String, String> uriQueryParameters) {
        final boolean aggregateBalanceOfSubOffices = UriQueryParameterHelper.
                aggregateBalanceOfSubOffices(uriQueryParameters);
        final Long officeId = UriQueryParameterHelper.getOfficeId(uriQueryParameters);
        final Long startClosureId = UriQueryParameterHelper.getStartClosureId(uriQueryParameters);
        final Long endClosureId = UriQueryParameterHelper.getEndClosureId(uriQueryParameters);
        final String reference = UriQueryParameterHelper.getReference(uriQueryParameters);
        
        GLClosureData endClosure = null;
        GLClosureData startClosure = null;
        
        if (endClosureId != null) {
            endClosure = this.glClosureReadPlatformService.retrieveGLClosureById(endClosureId);
        }
        
        if (startClosureId != null) {
            startClosure = this.glClosureReadPlatformService.retrieveGLClosureById(startClosureId);
        }
        
        this.glClosureJournalEntryBalanceValidator.validateGenerateReportRequest(officeId, 
                startClosure, endClosure);
        
        Collection<Long> officeIds = new ArrayList<Long>();
        
        // add the value of the "officeId" variable to the array list
        officeIds.add(officeId);
        
        if (aggregateBalanceOfSubOffices) {
            officeIds = this.officeReadPlatformService.officeByHierarchy(officeId);
        }
        
        final GLClosureAccountBalanceReportMapper mapper = new GLClosureAccountBalanceReportMapper(
                endClosure, startClosure, reference);
        final String sql = "select " + mapper.sql();
        
        MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource();
        
        String startClosureClosingDate = null;
        String endClosureClosingDate = this.mysqlDateFormatter.print(endClosure.getClosingDate());
        
        if (startClosure != null) {
            startClosureClosingDate = this.mysqlDateFormatter.print(startClosure.getClosingDate());
        }
        
        sqlParameterSource.addValue("officeIds", officeIds);
        sqlParameterSource.addValue("startClosureClosingDate", startClosureClosingDate);
        sqlParameterSource.addValue("endClosureClosingDate", endClosureClosingDate);
        
        final Collection<GLClosureAccountBalanceReportData> reportDataList = this.namedParameterJdbcTemplate.query(sql, 
                sqlParameterSource, mapper);
        
        return this.createGLClosureAccountBalanceReportCsvFile(reportDataList);
    }
    
    /**
     * Create the csv file with the balance report data
     * 
     * @param reportDataList
     * @return {@link File} object
     */
    private File createGLClosureAccountBalanceReportCsvFile(
            final Collection<GLClosureAccountBalanceReportData> reportDataList) {
        File file = null;
        
        try {
            final String fileDirectory = FileSystemContentRepository.MIFOSX_BASE_DIR + File.separator + "";
            
            if (!new File(fileDirectory).isDirectory()) {
                new File(fileDirectory).mkdirs();
            }
            
            file = new File(fileDirectory + "gl_closure_account_balance_report.csv");
            
            // use FileWriter constructor that specifies open for appending
            CsvWriter csvWriter = new CsvWriter(new FileWriter(file), ',');
            
            csvWriter.write("AccountCostCentre");
            csvWriter.write("AccountDepartment");
            csvWriter.write("AccountNumber");
            csvWriter.write("TransactionType");
            csvWriter.write("TransactionDate");
            csvWriter.write("GoodsAmount");
            csvWriter.write("Reference");
            csvWriter.write("Narrative");
            csvWriter.write("UniqueReferenceNumber");
            csvWriter.write("UserNumber");
            csvWriter.write("Source");
            csvWriter.write("PostedDate");
            csvWriter.write("TransactionAnalysisCode");
            csvWriter.endRecord();
            
            for (GLClosureAccountBalanceReportData reportData : reportDataList) {
                String transactionType = "";
                String transactionDate = "";
                String goodsAmount = "";
                String postedDate = "";
                
                if (reportData.getTransactionType() != null) {
                    transactionType = reportData.getTransactionType().getValue().toString();
                }
                
                if (reportData.getTransactionDate() != null) {
                    transactionDate = this.fileOutputDateFormatter.print(reportData.getTransactionDate());
                }
                
                if (reportData.getAmount() != null) {
                    goodsAmount = reportData.getAmount().setScale(2, RoundingMode.CEILING).
                            stripTrailingZeros().toPlainString();
                }
                
                if (reportData.getPostedDate() != null) {
                    postedDate = this.fileOutputDateFormatter.print(reportData.getPostedDate());
                }
                
                csvWriter.write("");
                csvWriter.write("");
                csvWriter.write(reportData.getAccountNumber());
                csvWriter.write(transactionType);
                csvWriter.write(transactionDate);
                csvWriter.write(goodsAmount);
                csvWriter.write(reportData.getReference());
                csvWriter.write("");
                csvWriter.write("");
                csvWriter.write("");
                csvWriter.write("");
                csvWriter.write(postedDate);
                csvWriter.write("");
                csvWriter.endRecord();
            }
            
            csvWriter.close();
        }
        
        catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }
        
        return file;
    }
    
    /**
     * Create the excel file with the balance report data
     * 
     * @param reportDataList
     * @return {@link File} object
     */
    @SuppressWarnings("unused")
    private File createGLClosureAccountBalanceReportExcelFile(
            final Collection<GLClosureAccountBalanceReportData> reportDataList) {
        File file = null;
        
        try {
            if (reportDataList != null) {
                final String[] columnTitles = new String[13];
                
                columnTitles[0] = "AccountCostCentre";
                columnTitles[1] = "AccountDepartment";
                columnTitles[2] = "AccountNumber";
                columnTitles[3] = "TransactionType";
                columnTitles[4] = "TransactionDate";
                columnTitles[5] = "GoodsAmount";
                columnTitles[6] = "Reference";
                columnTitles[7] = "Narrative";
                columnTitles[8] = "UniqueReferenceNumber";
                columnTitles[9] = "UserNumber";
                columnTitles[10] = "Source";
                columnTitles[11] = "PostedDate";
                columnTitles[12] = "TransactionAnalysisCode";
                
                XSSFWorkbook workbook = new XSSFWorkbook();
                XSSFSheet spreadsheet = workbook.createSheet(" nominaltransactions ");
                XSSFRow row;
                XSSFFont font;
                XSSFCellStyle style;
                XSSFDataFormat dataFormat;
                
                int rowId = 0;
                int cellId = 0;
                
                row = spreadsheet.createRow(rowId++);
                
                for (String columnTitle : columnTitles) {
                    font = workbook.createFont();
                    style = workbook.createCellStyle();
                    
                    font.setBold(true);
                    font.setFontName("Arial");
                    font.setFontHeightInPoints((short) 10);
                    style.setFont(font);
                    
                    Cell cell = row.createCell(cellId++);
                    
                    cell.setCellValue(columnTitle);
                    cell.setCellStyle(style);
                }
                
                for (GLClosureAccountBalanceReportData reportData : reportDataList) {
                    row = spreadsheet.createRow(rowId++);
                    font = workbook.createFont();
                    dataFormat = workbook.createDataFormat();
                    
                    font.setFontName("Arial");
                    font.setFontHeightInPoints((short) 10);
                    font.setBold(false);
                    
                    // ====================================================
                    Cell cell = row.createCell(2);
                    style = workbook.createCellStyle();
                    
                    cell.setCellType(Cell.CELL_TYPE_STRING);
                    style.setDataFormat(dataFormat.getFormat("@"));
                    style.setFont(font);
                    cell.setCellValue(reportData.getAccountNumber());
                    cell.setCellStyle(style);
                    // ====================================================
                        
                    // ====================================================
                    if (reportData.getTransactionType() != null) {
                        cell = row.createCell(3);
                        style = workbook.createCellStyle();
                        
                        style.setFont(font);
                        cell.setCellType(Cell.CELL_TYPE_NUMERIC);
                        cell.setCellValue(reportData.getTransactionType().getValue());
                        cell.setCellStyle(style);
                    }
                    // ====================================================
                    
                    // ====================================================
                    if (reportData.getTransactionDate() != null) {
                        cell = row.createCell(4);
                        style = workbook.createCellStyle();
                        
                        Date transactionDate = reportData.getTransactionDate().toDate();
                        
                        style.setDataFormat(dataFormat.getFormat("MM/DD/YY"));
                        style.setFont(font);
                        cell.setCellValue(transactionDate);
                        cell.setCellStyle(style);
                    }
                    // ====================================================
                    
                    // ====================================================
                    if (reportData.getAmount() != null) {
                        cell = row.createCell(5);
                        style = workbook.createCellStyle();
                        
                        Double amount = reportData.getAmount().doubleValue();
                        
                        style.setDataFormat(dataFormat.getFormat("0.00"));
                        style.setFont(font);
                        cell.setCellType(Cell.CELL_TYPE_NUMERIC);
                        cell.setCellValue(amount);
                        cell.setCellStyle(style);
                    }
                    // ====================================================
                    
                    // ====================================================
                    if (reportData.getReference() != null) {
                        cell = row.createCell(6);
                        style = workbook.createCellStyle();
                        
                        cell.setCellType(Cell.CELL_TYPE_STRING);
                        style.setDataFormat(dataFormat.getFormat("@"));
                        style.setFont(font);
                        cell.setCellValue(reportData.getReference());
                        cell.setCellStyle(style);
                    }
                    // ====================================================
                    
                    // ====================================================
                    if (reportData.getPostedDate() != null) {
                        cell = row.createCell(11);
                        style = workbook.createCellStyle();
                        
                        Date postedDate = reportData.getPostedDate().toDate();
                        
                        style.setDataFormat(dataFormat.getFormat("MM/DD/YY"));
                        style.setFont(font);
                        cell.setCellValue(postedDate);
                        cell.setCellStyle(style);
                    }
                    // ====================================================
                }
                
                final String fileDirectory = FileSystemContentRepository.MIFOSX_BASE_DIR + File.separator + "";
                
                if (!new File(fileDirectory).isDirectory()) {
                    new File(fileDirectory).mkdirs();
                }
                
                file = new File(fileDirectory + "gl_closure_account_balance_report.xls");
                
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                
                workbook.write(fileOutputStream);
                
                fileOutputStream.close();
            }
        }
        
        catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }
        
        return file;
    }
}
