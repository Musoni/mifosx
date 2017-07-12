  INSERT INTO stretchy_report (report_name,report_type,report_category,report_sql,description,core_report,use_report)
  VALUES ("Dashboard principal disbursed","Table","Dashboard","  select SUM(principal_disbursed_derived) as Db_principal_disbursed,  
officeId, staffId as staffId, monthYear

FROM (

	SELECT
	principal_disbursed_derived,
	DATE_FORMAT(ml.disbursedon_date,'%Y-%m-01') as monthYear,
     ounder.id as officeId,
      ml.loan_officer_id as staffId,
       ml.id as loanId

	FROM 
	m_office mo
	JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%')
	  AND ounder.hierarchy like CONCAT('.', '%')
	RIGHT JOIN m_client mc ON mc.office_id=ounder.id
	LEFT JOIN m_loan ml ON ml.client_id=mc.id
    left join m_staff st on st.id = ml.loan_officer_id

	WHERE mo.id=1 AND ml.disbursedon_date > LAST_DAY(CURDATE()-INTERVAL 12 MONTH) 
     and  ml.loan_officer_id IS NOT NULL

	UNION 

	SELECT
	principal_disbursed_derived,
	DATE_FORMAT(ml.disbursedon_date,'%Y-%m-01') as monthYear,
    ounder.id as officeId,
    ml.loan_officer_id as staffId,
    ml.id as loanId
    

	FROM 
	m_office mo
	JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%')
	  AND ounder.hierarchy like CONCAT('.', '%')
	RIGHT JOIN m_group mc ON mc.office_id=ounder.id
	LEFT JOIN m_loan ml ON ml.group_id=mc.id


	WHERE mo.id=1

	 AND ml.disbursedon_date > LAST_DAY(CURDATE()-INTERVAL 12 MONTH) 
     and  ml.loan_officer_id IS NOT NULL
 
 ) as t

GROUP BY officeId,staffId,monthYear 
order by monthYear","calculate ",0,0)