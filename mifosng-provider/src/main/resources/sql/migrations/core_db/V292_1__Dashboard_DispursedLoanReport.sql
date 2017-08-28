 INSERT INTO stretchy_report (report_name,report_type,report_category,report_sql,description,core_report,use_report)
  VALUES ("Dashboard principal disbursed","Table","Dashboard"," select SUM(principal_disbursed_derived) as principal_disbursed_loans,
officeId, staffId as staffId, DATE_FORMAT(NOW(),'%Y-%m-01') monthYear

FROM (

	select ounder.id as officeId, ml.id, mst.id as staffId, ml.principal_disbursed_derived
				FROM
				m_office mo
				JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%')
				AND ounder.hierarchy like CONCAT('.', '%')
				left join m_client mc on mc.office_id = ounder.id
				left join m_loan ml on ml.client_id = mc.id
				left join m_staff mst on mst.id = ml.loan_officer_id

				where  ml.disbursedon_date >= DATE_FORMAT(NOW(),'%Y-%m-01') and ml.disbursedon_date<=LAST_DAY(NOW())
				and mo.id = 1
				UNION


				select ounder.id as officeId, ml.id, mst.id as staffId, ml.principal_disbursed_derived
				FROM
				m_office mo
				JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%')
				AND ounder.hierarchy like CONCAT('.', '%')
				left join m_group mc on mc.office_id = ounder.id
				left join m_loan ml on ml.group_id = mc.id
				left join m_staff mst on mst.id = ml.loan_officer_id

				where  ml.disbursedon_date >= DATE_FORMAT(NOW(),'%Y-%m-01') and ml.disbursedon_date<=LAST_DAY(NOW())
				and mo.id = 1


 ) as t

GROUP BY officeId,staffId,monthYear
order by monthYear ","calculate ",0,0)