
 -- Dashboard number of outstanding loans

 INSERT INTO stretchy_report (report_name,report_type,report_category,report_sql,description,core_report,use_report)
  VALUES ("Dashboard number of outstanding loans","Table","Dashboard","  select count(id) as number_of_outstanding_loans,
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

				where  ( ml.closedon_date > LAST_DAY(NOW()) OR ml.closedon_date IS NULL )
				AND ( ml.writtenoffon_date > LAST_DAY(NOW()) OR ml.writtenoffon_date IS NULL )
				AND ml.disbursedon_date <=LAST_DAY(NOW())

				and mo.id = 1

				UNION


			select ounder.id as officeId, ml.id, mst.id as staffId, ml.principal_disbursed_derived
			FROM
				m_office mo
				JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%')
				AND ounder.hierarchy like CONCAT('.', '%')
				left join m_client mc on mc.office_id = ounder.id
				left join m_loan ml on ml.client_id = mc.id
				left join m_staff mst on mst.id = ml.loan_officer_id

				where  ( ml.closedon_date > LAST_DAY(NOW()) OR ml.closedon_date IS NULL )
				AND ( ml.writtenoffon_date > LAST_DAY(NOW()) OR ml.writtenoffon_date IS NULL )
				AND ml.disbursedon_date <=LAST_DAY(NOW())

				and mo.id = 1

 ) as t

GROUP BY officeId,staffId,monthYear ","calculate ",0,0);




 -- Dashboard principal outstanding

 INSERT INTO stretchy_report (report_name,report_type,report_category,report_sql,description,core_report,use_report)
  VALUES ("Dashboard principal outstanding","Table","Dashboard","  select SUM(POUT) as principal_outstanding,
officeId, staffId as staffId, DATE_FORMAT(NOW(),'%Y-%m-01') monthYear

FROM (

	select ounder.id as officeId, ml.id, mst.id as staffId, (ml.principal_disbursed_derived-IFNULL(mlt.principal_paid,0) ) as POUT
    		FROM
    			m_office mo
    			JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%')
    			AND ounder.hierarchy like CONCAT('.', '%')
    			left join m_client mc on mc.office_id = ounder.id AND mc.`activation_date` <= LAST_DAY(NOW()) and (mc.closedon_date is null or mc.closedon_date >= LAST_DAY(NOW()))
    			left join m_loan ml on ml.client_id = mc.id and ml.disbursedon_date <= LAST_DAY(NOW()) and ml.disbursedon_date <= LAST_DAY(NOW()) and (ml.closedon_date is null or ml.closedon_date > LAST_DAY(NOW()))
    			 join m_staff mst on mst.id = ml.loan_officer_id
    			left join
    				(
    					SELECT
    					loan_id,
    					SUM(IFNULL(principal_portion_derived,0)) as principal_paid,
    					SUM(IFNULL(interest_portion_derived,0)) as interest_paid
    					FROM m_loan_transaction as mlt
    					left join m_loan as ml on mlt.loan_id = ml.id
    					where is_reversed = 0
    					and  ml.disbursedon_date <= LAST_DAY(NOW()) and ml.disbursedon_date <= LAST_DAY(NOW())
    					and (ml.closedon_date is null or ml.closedon_date >LAST_DAY(NOW()))
    					and `transaction_type_enum` IN (2,5)
    					and transaction_date <= LAST_DAY(NOW())
    					group by loan_id
    				) as mlt on ml.id = mlt.loan_id

    			where  ( ml.closedon_date > LAST_DAY(NOW()) OR ml.closedon_date IS NULL )
    			AND ( ml.writtenoffon_date > LAST_DAY(NOW()) OR ml.writtenoffon_date IS NULL )
    			AND ml.disbursedon_date <=LAST_DAY(NOW())

    			and mo.id = 1

    			UNION


    			select ounder.id as officeId, ml.id, mst.id as staffId, (ml.principal_disbursed_derived-IFNULL(mlt.principal_paid,0) ) as POUT
    			FROM
    			m_office mo
    			JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%')
    			AND ounder.hierarchy like CONCAT('.', '%')
    			left join m_group mc on mc.office_id = ounder.id AND mc.`activation_date` <= LAST_DAY(NOW()) and (mc.closedon_date is null or mc.closedon_date >= LAST_DAY(NOW()))
    			left join m_loan ml on ml.group_id = mc.id and ml.disbursedon_date <= LAST_DAY(NOW()) and ml.disbursedon_date <= LAST_DAY(NOW()) and (ml.closedon_date is null or ml.closedon_date > LAST_DAY(NOW()))
    			 join m_staff mst on mst.id = ml.loan_officer_id

    			left join
    				(
    					SELECT
    					loan_id,
    					SUM(IFNULL(principal_portion_derived,0)) as principal_paid,
    					SUM(IFNULL(interest_portion_derived,0)) as interest_paid
    					FROM m_loan_transaction as mlt
    					left join m_loan as ml on mlt.loan_id = ml.id
    					where is_reversed = 0
    					and  ml.disbursedon_date <= LAST_DAY(NOW()) and ml.disbursedon_date <= LAST_DAY(NOW())
    					and (ml.closedon_date is null or ml.closedon_date > LAST_DAY(NOW()))
    					and `transaction_type_enum` IN (2,5)
    					and transaction_date <=LAST_DAY(NOW())
    					group by loan_id
    				) as mlt on ml.id = mlt.loan_id

    			where  ( ml.closedon_date > LAST_DAY(NOW()) OR ml.closedon_date IS NULL )
    			AND ( ml.writtenoffon_date > LAST_DAY(NOW()) OR ml.writtenoffon_date IS NULL )
    			AND ml.disbursedon_date <=LAST_DAY(NOW())

    			and mo.id = 1

 ) as t

GROUP BY officeId,staffId,monthYear
order by monthYear","calculate ",0,0);





 -- Dashboard interest outstanding

 INSERT INTO stretchy_report (report_name,report_type,report_category,report_sql,description,core_report,use_report)
  VALUES ("Dashboard interest outstanding","Table","Dashboard"," select SUM(IOUT) as interest_outstanding,
officeId, staffId as staffId, DATE_FORMAT(NOW(),'%Y-%m-01') monthYear

FROM (

		select ounder.id as officeId, ml.id, mst.id as staffId, (ml.interest_charged_derived-IFNULL(mlt.interest_paid,0) - IFNULL(mlw.waived_interest,0)) as IOUT
    		FROM
    			m_office mo
    			JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%')
    			AND ounder.hierarchy like CONCAT('.', '%')
    			left join m_client mc on mc.office_id = ounder.id AND mc.`activation_date` <= LAST_DAY(NOW()) and (mc.closedon_date is null or mc.closedon_date >= LAST_DAY(NOW()))
    			left join m_loan ml on ml.client_id = mc.id and ml.disbursedon_date <= LAST_DAY(NOW()) and ml.disbursedon_date <= LAST_DAY(NOW()) and (ml.closedon_date is null or ml.closedon_date > LAST_DAY(NOW()))
    			 join m_staff mst on mst.id = ml.loan_officer_id
    			left join
    				(
    					SELECT
    					loan_id,
    					SUM(IFNULL(principal_portion_derived,0)) as principal_paid,
    					SUM(IFNULL(interest_portion_derived,0)) as interest_paid
    					FROM m_loan_transaction as mlt
    					left join m_loan as ml on mlt.loan_id = ml.id
    					where is_reversed = 0
    					and  ml.disbursedon_date <= LAST_DAY(NOW()) and ml.disbursedon_date <= LAST_DAY(NOW())
    					and (ml.closedon_date is null or ml.closedon_date >LAST_DAY(NOW()))
    					and `transaction_type_enum` IN (2,5)
    					and transaction_date <= LAST_DAY(NOW())
    					group by loan_id
    				) as mlt on ml.id = mlt.loan_id
    
                    LEFT JOIN
    			 (
    				SELECT
    				loan_id,
    				SUM(IFNULL(interest_waived_derived,0)) as waived_interest,
    				SUM(IFNULL(fee_charges_waived_derived,0)) as waived_fee_charges,
    				SUM(IFNULL(penalty_charges_waived_derived,0)) as waived_penalty_charges,
    				IFNULL((SUM(IFNULL(interest_waived_derived,0)) + SUM(IFNULL(fee_charges_waived_derived,0)) + SUM(IFNULL(penalty_charges_waived_derived,0))),0) AS total_waived
    				FROM
    				m_loan_repayment_schedule
    				group by loan_id
    			) as mlw on ml.id = mlw.loan_id
    
    			where  ( ml.closedon_date > LAST_DAY(NOW()) OR ml.closedon_date IS NULL )
    			AND ( ml.writtenoffon_date > LAST_DAY(NOW()) OR ml.writtenoffon_date IS NULL )
    			AND ml.disbursedon_date <=LAST_DAY(NOW())
    
    			and mo.id = 1
    
    			UNION ALL
    
    
    			select ounder.id as officeId, ml.id, mst.id as staffId, (ml.interest_charged_derived-IFNULL(mlt.interest_paid,0)-IFNULL(interest_waived_derived,0) ) as POUT
    			FROM
    			m_office mo
    			JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%')
    			AND ounder.hierarchy like CONCAT('.', '%')
    			left join m_group mc on mc.office_id = ounder.id AND mc.`activation_date` <= LAST_DAY(NOW()) and (mc.closedon_date is null or mc.closedon_date >= LAST_DAY(NOW()))
    			left join m_loan ml on ml.group_id = mc.id and ml.disbursedon_date <= LAST_DAY(NOW()) and ml.disbursedon_date <= LAST_DAY(NOW()) and (ml.closedon_date is null or ml.closedon_date > LAST_DAY(NOW()))
    			 join m_staff mst on mst.id = ml.loan_officer_id
    
    			left join
    				(
    					SELECT
    					loan_id,
    					SUM(IFNULL(principal_portion_derived,0)) as principal_paid,
    					SUM(IFNULL(interest_portion_derived,0)) as interest_paid
    					FROM m_loan_transaction as mlt
    					left join m_loan as ml on mlt.loan_id = ml.id
    					where is_reversed = 0
    					and  ml.disbursedon_date <= LAST_DAY(NOW()) and ml.disbursedon_date <= LAST_DAY(NOW())
    					and (ml.closedon_date is null or ml.closedon_date > LAST_DAY(NOW()))
    					and `transaction_type_enum` IN (2,5)
    					and transaction_date <=LAST_DAY(NOW())
    					group by loan_id
    				) as mlt on ml.id = mlt.loan_id
                       LEFT JOIN
    			 (
    				SELECT
    				loan_id,
    				SUM(IFNULL(interest_waived_derived,0)) as waived_interest,
    				SUM(IFNULL(fee_charges_waived_derived,0)) as waived_fee_charges,
    				SUM(IFNULL(penalty_charges_waived_derived,0)) as waived_penalty_charges,
    				IFNULL((SUM(IFNULL(interest_waived_derived,0)) + SUM(IFNULL(fee_charges_waived_derived,0)) + SUM(IFNULL(penalty_charges_waived_derived,0))),0) AS total_waived
    				FROM
    				m_loan_repayment_schedule
    				group by loan_id
    			) as mlw on ml.id = mlw.loan_id
    
    			where  ( ml.closedon_date > LAST_DAY(NOW()) OR ml.closedon_date IS NULL )
    			AND ( ml.writtenoffon_date > LAST_DAY(NOW()) OR ml.writtenoffon_date IS NULL )
    			AND ml.disbursedon_date <=LAST_DAY(NOW())
    
    			and mo.id = 1

 ) as t

GROUP BY officeId,staffId,monthYear
order by monthYear","calculate ",0,0);



 -- Dashboard PAR_1

 INSERT INTO stretchy_report (report_name,report_type,report_category,report_sql,description,core_report,use_report)
  VALUES ("Dashboard PAR_1","Table","Dashboard","  select SUM(PAROLB_1) as PAR_1,
officeId, staffId as staffId, DATE_FORMAT(NOW(),'%Y-%m-01') monthYear

FROM (

	select ounder.id as officeId, ml.id, mst.id as staffId,
			(CASE WHEN IFNULL(days_in_arrears,0) > 0 THEN (IFNULL(ml.`principal_disbursed_derived`,0) - IFNULL(mlt.principal_paid,0)) ELSE 0 END) as PAROLB_1,
			(CASE WHEN IFNULL(days_in_arrears,0) > 30 THEN (IFNULL(ml.`principal_disbursed_derived`,0) - IFNULL(mlt.principal_paid,0)) ELSE 0 END) as PAROLB_31,
			(CASE WHEN IFNULL(days_in_arrears,0) BETWEEN 61 AND 90 THEN (IFNULL(ml.`principal_disbursed_derived`,0) - IFNULL(mlt.principal_paid,0)) ELSE 0 END) as PAROLB_90
			FROM
				m_office mo
				JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%')
				AND ounder.hierarchy like CONCAT('.', '%')
				left join m_client mc on mc.office_id = ounder.id
				left join m_loan ml on ml.client_id = mc.id
				left join m_staff mst on mst.id = ml.loan_officer_id
				left join
					(
						SELECT
						loan_id,
						SUM(IFNULL(principal_portion_derived,0)) as principal_paid,
						SUM(IFNULL(interest_portion_derived,0)) as interest_paid
						FROM m_loan_transaction as mlt
						left join m_loan as ml on mlt.loan_id = ml.id
						where is_reversed = 0
						and  ml.disbursedon_date <= LAST_DAY(NOW()) and ml.disbursedon_date <= LAST_DAY(NOW())
						and (ml.closedon_date is null or ml.closedon_date > LAST_DAY(NOW()))
						and `transaction_type_enum` IN (2,5)
						and transaction_date <= LAST_DAY(NOW())
						group by loan_id
					) as mlt on ml.id = mlt.loan_id
					 left JOIN
					(
						SELECT
						DATEDIFF(DATE(LAST_DAY(NOW())), duedate) as days_in_arrears,
						lrs.duedate as arrears_duedate,
						lrs.loan_id
						FROM m_loan_repayment_schedule lrs
						WHERE `duedate` <  LAST_DAY(NOW())
						and (( obligations_met_on_date > duedate
						and obligations_met_on_date > LAST_DAY(NOW()))
						OR (obligations_met_on_date is null))
						GROUP BY loan_id HAVING duedate = MIN(duedate)
					) as arr ON ml.id = arr.loan_id

				where  ( ml.closedon_date > LAST_DAY(NOW()) OR ml.closedon_date IS NULL )
				AND ( ml.writtenoffon_date > LAST_DAY(NOW()) OR ml.writtenoffon_date IS NULL )
				AND ml.disbursedon_date <=LAST_DAY(NOW())

				and mo.id = 1

				UNION


			select ounder.id as officeId, ml.id, mst.id as staffId,
			(CASE WHEN IFNULL(days_in_arrears,0) > 0 THEN (IFNULL(ml.`principal_disbursed_derived`,0) - IFNULL(mlt.principal_paid,0)) ELSE 0 END) as PAROLB_1,
			(CASE WHEN IFNULL(days_in_arrears,0) > 30 THEN (IFNULL(ml.`principal_disbursed_derived`,0) - IFNULL(mlt.principal_paid,0)) ELSE 0 END) as PAROLB_31,
			(CASE WHEN IFNULL(days_in_arrears,0) BETWEEN 61 AND 90 THEN (IFNULL(ml.`principal_disbursed_derived`,0) - IFNULL(mlt.principal_paid,0)) ELSE 0 END) as PAROLB_90
			FROM
				m_office mo
				JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%')
				AND ounder.hierarchy like CONCAT('.', '%')
				left join m_group mc on mc.office_id = ounder.id
				left join m_loan ml on ml.group_id = mc.id
				left join m_staff mst on mst.id = ml.loan_officer_id

				left join
					(
						SELECT
						loan_id,
						SUM(IFNULL(principal_portion_derived,0)) as principal_paid,
						SUM(IFNULL(interest_portion_derived,0)) as interest_paid
						FROM m_loan_transaction as mlt
						left join m_loan as ml on mlt.loan_id = ml.id
						where is_reversed = 0
						and  ml.disbursedon_date <= LAST_DAY(NOW()) and ml.disbursedon_date <= LAST_DAY(NOW())
						and (ml.closedon_date is null or ml.closedon_date > LAST_DAY(NOW()))
						and `transaction_type_enum` IN (2,5)
						and transaction_date <= LAST_DAY(NOW())
						group by loan_id
					) as mlt on ml.id = mlt.loan_id

					left JOIN
					(
						SELECT
						DATEDIFF(DATE(LAST_DAY(NOW())), duedate) as days_in_arrears,
						lrs.duedate as arrears_duedate,
						lrs.loan_id
						FROM m_loan_repayment_schedule lrs
						WHERE `duedate` <  LAST_DAY(NOW())
						and (( obligations_met_on_date > duedate
						and obligations_met_on_date > LAST_DAY(NOW()))
						OR (obligations_met_on_date is null))
						GROUP BY loan_id HAVING duedate = MIN(duedate)
					) as arr ON ml.id = arr.loan_id

				where  ( ml.closedon_date > LAST_DAY(NOW()) OR ml.closedon_date IS NULL )
				AND ( ml.writtenoffon_date > LAST_DAY(NOW()) OR ml.writtenoffon_date IS NULL )
				AND ml.disbursedon_date <=LAST_DAY(NOW())
				and mo.id = 1

 ) as t

GROUP BY officeId,staffId,monthYear
order by monthYear","calculate ",0,0);


 -- Dashboard PAR_30

 INSERT INTO stretchy_report (report_name,report_type,report_category,report_sql,description,core_report,use_report)
  VALUES ("Dashboard PAR_30","Table","Dashboard","  select SUM(PAROLB_31) as PAR_30,
officeId, staffId as staffId, DATE_FORMAT(NOW(),'%Y-%m-01') monthYear

FROM (

	select ounder.id as officeId, ml.id, mst.id as staffId,
			(CASE WHEN IFNULL(days_in_arrears,0) > 0 THEN (IFNULL(ml.`principal_disbursed_derived`,0) - IFNULL(mlt.principal_paid,0)) ELSE 0 END) as PAROLB_1,
			(CASE WHEN IFNULL(days_in_arrears,0) > 30 THEN (IFNULL(ml.`principal_disbursed_derived`,0) - IFNULL(mlt.principal_paid,0)) ELSE 0 END) as PAROLB_31,
			(CASE WHEN IFNULL(days_in_arrears,0) BETWEEN 61 AND 90 THEN (IFNULL(ml.`principal_disbursed_derived`,0) - IFNULL(mlt.principal_paid,0)) ELSE 0 END) as PAROLB_90
			FROM
				m_office mo
				JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%')
				AND ounder.hierarchy like CONCAT('.', '%')
				left join m_client mc on mc.office_id = ounder.id
				left join m_loan ml on ml.client_id = mc.id
				left join m_staff mst on mst.id = ml.loan_officer_id
				left join
					(
						SELECT
						loan_id,
						SUM(IFNULL(principal_portion_derived,0)) as principal_paid,
						SUM(IFNULL(interest_portion_derived,0)) as interest_paid
						FROM m_loan_transaction as mlt
						left join m_loan as ml on mlt.loan_id = ml.id
						where is_reversed = 0
						and  ml.disbursedon_date <= LAST_DAY(NOW()) and ml.disbursedon_date <= LAST_DAY(NOW())
						and (ml.closedon_date is null or ml.closedon_date > LAST_DAY(NOW()))
						and `transaction_type_enum` IN (2,5)
						and transaction_date <= LAST_DAY(NOW())
						group by loan_id
					) as mlt on ml.id = mlt.loan_id
					 left JOIN
					(
						SELECT
						DATEDIFF(DATE(LAST_DAY(NOW())), duedate) as days_in_arrears,
						lrs.duedate as arrears_duedate,
						lrs.loan_id
						FROM m_loan_repayment_schedule lrs
						WHERE `duedate` <  LAST_DAY(NOW())
						and (( obligations_met_on_date > duedate
						and obligations_met_on_date > LAST_DAY(NOW()))
						OR (obligations_met_on_date is null))
						GROUP BY loan_id HAVING duedate = MIN(duedate)
					) as arr ON ml.id = arr.loan_id

				where  ( ml.closedon_date > LAST_DAY(NOW()) OR ml.closedon_date IS NULL )
				AND ( ml.writtenoffon_date > LAST_DAY(NOW()) OR ml.writtenoffon_date IS NULL )
				AND ml.disbursedon_date <=LAST_DAY(NOW())

				and mo.id = 1

				UNION


			select ounder.id as officeId, ml.id, mst.id as staffId,
			(CASE WHEN IFNULL(days_in_arrears,0) > 0 THEN (IFNULL(ml.`principal_disbursed_derived`,0) - IFNULL(mlt.principal_paid,0)) ELSE 0 END) as PAROLB_1,
			(CASE WHEN IFNULL(days_in_arrears,0) > 30 THEN (IFNULL(ml.`principal_disbursed_derived`,0) - IFNULL(mlt.principal_paid,0)) ELSE 0 END) as PAROLB_31,
			(CASE WHEN IFNULL(days_in_arrears,0) BETWEEN 61 AND 90 THEN (IFNULL(ml.`principal_disbursed_derived`,0) - IFNULL(mlt.principal_paid,0)) ELSE 0 END) as PAROLB_90
			FROM
				m_office mo
				JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%')
				AND ounder.hierarchy like CONCAT('.', '%')
				left join m_group mc on mc.office_id = ounder.id
				left join m_loan ml on ml.group_id = mc.id
				left join m_staff mst on mst.id = ml.loan_officer_id

				left join
					(
						SELECT
						loan_id,
						SUM(IFNULL(principal_portion_derived,0)) as principal_paid,
						SUM(IFNULL(interest_portion_derived,0)) as interest_paid
						FROM m_loan_transaction as mlt
						left join m_loan as ml on mlt.loan_id = ml.id
						where is_reversed = 0
						and  ml.disbursedon_date <= LAST_DAY(NOW()) and ml.disbursedon_date <= LAST_DAY(NOW())
						and (ml.closedon_date is null or ml.closedon_date > LAST_DAY(NOW()))
						and `transaction_type_enum` IN (2,5)
						and transaction_date <= LAST_DAY(NOW())
						group by loan_id
					) as mlt on ml.id = mlt.loan_id

					left JOIN
					(
						SELECT
						DATEDIFF(DATE(LAST_DAY(NOW())), duedate) as days_in_arrears,
						lrs.duedate as arrears_duedate,
						lrs.loan_id
						FROM m_loan_repayment_schedule lrs
						WHERE `duedate` <  LAST_DAY(NOW())
						and (( obligations_met_on_date > duedate
						and obligations_met_on_date > LAST_DAY(NOW()))
						OR (obligations_met_on_date is null))
						GROUP BY loan_id HAVING duedate = MIN(duedate)
					) as arr ON ml.id = arr.loan_id

				where  ( ml.closedon_date > LAST_DAY(NOW()) OR ml.closedon_date IS NULL )
				AND ( ml.writtenoffon_date > LAST_DAY(NOW()) OR ml.writtenoffon_date IS NULL )
				AND ml.disbursedon_date <=LAST_DAY(NOW())
				and mo.id = 1

 ) as t

GROUP BY officeId,staffId,monthYear
order by monthYear","calculate ",0,0);



 -- Dashboard PAR_90

 INSERT INTO stretchy_report (report_name,report_type,report_category,report_sql,description,core_report,use_report)
  VALUES ("Dashboard PAR_90","Table","Dashboard","  select SUM(PAROLB_90) as PAR_90,
officeId, staffId as staffId, DATE_FORMAT(NOW(),'%Y-%m-01') monthYear

FROM (

	select ounder.id as officeId, ml.id, mst.id as staffId,
			(CASE WHEN IFNULL(days_in_arrears,0) > 0 THEN (IFNULL(ml.`principal_disbursed_derived`,0) - IFNULL(mlt.principal_paid,0)) ELSE 0 END) as PAROLB_1,
			(CASE WHEN IFNULL(days_in_arrears,0) > 30 THEN (IFNULL(ml.`principal_disbursed_derived`,0) - IFNULL(mlt.principal_paid,0)) ELSE 0 END) as PAROLB_31,
			(CASE WHEN IFNULL(days_in_arrears,0) BETWEEN 61 AND 90 THEN (IFNULL(ml.`principal_disbursed_derived`,0) - IFNULL(mlt.principal_paid,0)) ELSE 0 END) as PAROLB_90
			FROM
				m_office mo
				JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%')
				AND ounder.hierarchy like CONCAT('.', '%')
				left join m_client mc on mc.office_id = ounder.id
				left join m_loan ml on ml.client_id = mc.id
				left join m_staff mst on mst.id = ml.loan_officer_id
				left join
					(
						SELECT
						loan_id,
						SUM(IFNULL(principal_portion_derived,0)) as principal_paid,
						SUM(IFNULL(interest_portion_derived,0)) as interest_paid
						FROM m_loan_transaction as mlt
						left join m_loan as ml on mlt.loan_id = ml.id
						where is_reversed = 0
						and  ml.disbursedon_date <= LAST_DAY(NOW()) and ml.disbursedon_date <= LAST_DAY(NOW())
						and (ml.closedon_date is null or ml.closedon_date > LAST_DAY(NOW()))
						and `transaction_type_enum` IN (2,5)
						and transaction_date <= LAST_DAY(NOW())
						group by loan_id
					) as mlt on ml.id = mlt.loan_id
					 left JOIN
					(
						SELECT
						DATEDIFF(DATE(LAST_DAY(NOW())), duedate) as days_in_arrears,
						lrs.duedate as arrears_duedate,
						lrs.loan_id
						FROM m_loan_repayment_schedule lrs
						WHERE `duedate` <  LAST_DAY(NOW())
						and (( obligations_met_on_date > duedate
						and obligations_met_on_date > LAST_DAY(NOW()))
						OR (obligations_met_on_date is null))
						GROUP BY loan_id HAVING duedate = MIN(duedate)
					) as arr ON ml.id = arr.loan_id

				where  ( ml.closedon_date > LAST_DAY(NOW()) OR ml.closedon_date IS NULL )
				AND ( ml.writtenoffon_date > LAST_DAY(NOW()) OR ml.writtenoffon_date IS NULL )
				AND ml.disbursedon_date <=LAST_DAY(NOW())

				and mo.id = 1

				UNION


			select ounder.id as officeId, ml.id, mst.id as staffId,
			(CASE WHEN IFNULL(days_in_arrears,0) > 0 THEN (IFNULL(ml.`principal_disbursed_derived`,0) - IFNULL(mlt.principal_paid,0)) ELSE 0 END) as PAROLB_1,
			(CASE WHEN IFNULL(days_in_arrears,0) > 30 THEN (IFNULL(ml.`principal_disbursed_derived`,0) - IFNULL(mlt.principal_paid,0)) ELSE 0 END) as PAROLB_31,
			(CASE WHEN IFNULL(days_in_arrears,0) BETWEEN 61 AND 90 THEN (IFNULL(ml.`principal_disbursed_derived`,0) - IFNULL(mlt.principal_paid,0)) ELSE 0 END) as PAROLB_90
			FROM
				m_office mo
				JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%')
				AND ounder.hierarchy like CONCAT('.', '%')
				left join m_group mc on mc.office_id = ounder.id
				left join m_loan ml on ml.group_id = mc.id
				left join m_staff mst on mst.id = ml.loan_officer_id

				left join
					(
						SELECT
						loan_id,
						SUM(IFNULL(principal_portion_derived,0)) as principal_paid,
						SUM(IFNULL(interest_portion_derived,0)) as interest_paid
						FROM m_loan_transaction as mlt
						left join m_loan as ml on mlt.loan_id = ml.id
						where is_reversed = 0
						and  ml.disbursedon_date <= LAST_DAY(NOW()) and ml.disbursedon_date <= LAST_DAY(NOW())
						and (ml.closedon_date is null or ml.closedon_date > LAST_DAY(NOW()))
						and `transaction_type_enum` IN (2,5)
						and transaction_date <= LAST_DAY(NOW())
						group by loan_id
					) as mlt on ml.id = mlt.loan_id

					left JOIN
					(
						SELECT
						DATEDIFF(DATE(LAST_DAY(NOW())), duedate) as days_in_arrears,
						lrs.duedate as arrears_duedate,
						lrs.loan_id
						FROM m_loan_repayment_schedule lrs
						WHERE `duedate` <  LAST_DAY(NOW())
						and (( obligations_met_on_date > duedate
						and obligations_met_on_date > LAST_DAY(NOW()))
						OR (obligations_met_on_date is null))
						GROUP BY loan_id HAVING duedate = MIN(duedate)
					) as arr ON ml.id = arr.loan_id

				where  ( ml.closedon_date > LAST_DAY(NOW()) OR ml.closedon_date IS NULL )
				AND ( ml.writtenoffon_date > LAST_DAY(NOW()) OR ml.writtenoffon_date IS NULL )
				AND ml.disbursedon_date <=LAST_DAY(NOW())
				and mo.id = 1

 ) as t

GROUP BY officeId,staffId,monthYear
order by monthYear","calculate ",0,0);


--  OLB


 INSERT INTO stretchy_report (report_name,report_type,report_category,report_sql,description,core_report,use_report)
  VALUES ("Dashboard POLB","Table","Dashboard","  select SUM(OLB) as POLB,
officeId, staffId as staffId, DATE_FORMAT(NOW(),'%Y-%m-01') monthYear

FROM (

	select ounder.id as officeId, ml.id, mst.id as staffId,
    			 (IFNULL(ml.`principal_disbursed_derived`,0) - IFNULL(mlt.principal_paid,0)) as OLB
    				from m_office mo
    				JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%')
    				AND ounder.hierarchy like CONCAT('.', '%')
    				left join m_client mc on mc.office_id = ounder.id
    				left join m_loan ml on ml.client_id = mc.id
    				left join m_staff mst on mst.id = ml.loan_officer_id
    				left join
    					(
    						SELECT
    						loan_id,
    						SUM(IFNULL(principal_portion_derived,0)) as principal_paid,
    						SUM(IFNULL(interest_portion_derived,0)) as interest_paid
    						FROM m_loan_transaction as mlt
    						left join m_loan as ml on mlt.loan_id = ml.id
    						where is_reversed = 0
    						and  ml.disbursedon_date <= LAST_DAY(NOW()) and ml.disbursedon_date <= LAST_DAY(NOW())
    						and (ml.closedon_date is null or ml.closedon_date > LAST_DAY(NOW()))
    						and `transaction_type_enum` IN (2,5)
    						and transaction_date <=LAST_DAY(NOW())
    						group by loan_id
    					) as mlt on ml.id = mlt.loan_id
    					 left JOIN
    					(
    						SELECT
    						DATEDIFF(DATE(LAST_DAY(NOW())), duedate) as days_in_arrears,
    						lrs.duedate as arrears_duedate,
    						lrs.loan_id
    						FROM m_loan_repayment_schedule lrs
    						WHERE `duedate` <  LAST_DAY(NOW())
    						and (( obligations_met_on_date > duedate
    						and obligations_met_on_date > LAST_DAY(NOW()))
    						OR (obligations_met_on_date is null))
    						GROUP BY loan_id HAVING duedate = MIN(duedate)
    					) as arr ON ml.id = arr.loan_id

    				where  ( ml.closedon_date > LAST_DAY(NOW()) OR ml.closedon_date IS NULL )
    				AND ( ml.writtenoffon_date > LAST_DAY(NOW()) OR ml.writtenoffon_date IS NULL )
    				AND ml.disbursedon_date <=LAST_DAY(NOW())

    				and mo.id = 1

    				UNION


    			select ounder.id as officeId, ml.id, mst.id as staffId,
    			 (IFNULL(ml.`principal_disbursed_derived`,0) - IFNULL(mlt.principal_paid,0)) as OLB
    			FROM
    				m_office mo
    				JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%')
    				AND ounder.hierarchy like CONCAT('.', '%')
    				left join m_group mc on mc.office_id = ounder.id
    				left join m_loan ml on ml.group_id = mc.id
    				left join m_staff mst on mst.id = ml.loan_officer_id

    				left join
    					(
    						SELECT
    						loan_id,
    						SUM(IFNULL(principal_portion_derived,0)) as principal_paid,
    						SUM(IFNULL(interest_portion_derived,0)) as interest_paid
    						FROM m_loan_transaction as mlt
    						left join m_loan as ml on mlt.loan_id = ml.id
    						where is_reversed = 0
    						and  ml.disbursedon_date <= LAST_DAY(NOW()) and ml.disbursedon_date <= LAST_DAY(NOW())
    						and (ml.closedon_date is null or ml.closedon_date >LAST_DAY(NOW()))
    						and `transaction_type_enum` IN (2,5)
    						and transaction_date <= LAST_DAY(NOW())
    						group by loan_id
    					) as mlt on ml.id = mlt.loan_id

    					left JOIN
    					(
    						SELECT
    						DATEDIFF(DATE(LAST_DAY(NOW())), duedate) as days_in_arrears,
    						lrs.duedate as arrears_duedate,
    						lrs.loan_id
    						FROM m_loan_repayment_schedule lrs
    						WHERE `duedate` < LAST_DAY(NOW())
    						and (( obligations_met_on_date > duedate
    						and obligations_met_on_date > LAST_DAY(NOW()))
    						OR (obligations_met_on_date is null))
    						GROUP BY loan_id HAVING duedate = MIN(duedate)
    					) as arr ON ml.id = arr.loan_id

    				where  ( ml.closedon_date > LAST_DAY(NOW()) OR ml.closedon_date IS NULL )
    				AND ( ml.writtenoffon_date > LAST_DAY(NOW()) OR ml.writtenoffon_date IS NULL )
    				AND ml.disbursedon_date <=LAST_DAY(NOW())
    				and mo.id = 1


 ) as t

GROUP BY officeId,staffId,monthYear
order by monthYear","calculate ",0,0);


 -- Dashboard Write off

 INSERT INTO stretchy_report (report_name,report_type,report_category,report_sql,description,core_report,use_report)
  VALUES ("Dashboard Write off","Table","Dashboard"," select SUM(writteoff) as Writte_off,
officeId, staffId as staffId, DATE_FORMAT(NOW(),'%Y-%m-01') monthYear

FROM (

            select ounder.id as officeId, ml.id, mst.id as staffId,
                        IFNULL(`principal_writtenoff_derived`,0) as writteoff

            FROM
            m_office mo
            JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%')
            AND ounder.hierarchy like CONCAT('.', '%')
             join m_client mc on mc.office_id = ounder.id AND mc.`activation_date` <= LAST_DAY(NOW())
             join m_loan ml on ml.client_id = mc.id  AND ml.disbursedon_date <= LAST_DAY(NOW()) and ml.writtenoffon_date >= LAST_DAY(NOW()) and ml.writtenoffon_date <=LAST_DAY(NOW())
            left join m_staff mst on mst.id = ml.loan_officer_id


            where  ml.client_id is not null and  (ml.total_overpaid_derived = 0 OR ml.total_overpaid_derived is null)
            and mo.id = 1

            UNION


            select ounder.id as officeId, ml.id, mst.id as staffId,
            IFNULL(`principal_writtenoff_derived`,0) as writteoff

            FROM
            m_office mo
            JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%')
            AND ounder.hierarchy like CONCAT('.', '%')
             join m_group mc on mc.office_id = ounder.id AND mc.`activation_date` <= LAST_DAY(NOW())
             join m_loan ml on ml.group_id = mc.id AND ml.disbursedon_date <= LAST_DAY(NOW()) and ml.writtenoffon_date >= LAST_DAY(NOW()) and ml.writtenoffon_date <=LAST_DAY(NOW())
            left join m_staff mst on mst.id = ml.loan_officer_id


            where  ml.group_id is not null and  (ml.total_overpaid_derived = 0 OR ml.total_overpaid_derived is null)
            and mo.id = 1

 ) as t

GROUP BY officeId,staffId,monthYear
order by monthYear","calculate ",0,0);



 -- Dashboard Write off

 INSERT INTO stretchy_report (report_name,report_type,report_category,report_sql,description,core_report,use_report)
  VALUES ("Dashboard Repayments","Table","Dashboard","  select SUM(pif) as Repayments,
officeId, staffId as staffId, DATE_FORMAT(NOW(),'%Y-%m-01') monthYear

FROM (

	        select
			ounder.id as officeId, ml.id, s.id as staffId,
            ROUND((ifnull(lt.principal_portion_derived,0) + ifnull(lt.interest_portion_derived,0) + ifnull(lt.fee_charges_portion_derived,0) + ifnull(lt.penalty_charges_portion_derived,0) + ifnull(lt.overpayment_portion_derived,0)), 2) as pif

			FROM m_office mo
			JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%')
			AND ounder.hierarchy like CONCAT('.', '%')
			join m_client c on c.office_id=ounder.id
			left join m_loan ml on c.id = ml.client_id
			left join m_loan_transaction lt on lt.loan_id = ml.id and lt.is_reversed = 0
			and lt.transaction_type_enum IN (2,8)

			left join m_staff s on ml.loan_officer_id = s.id

			where lt.transaction_date  >= DATE_FORMAT(NOW(),'%Y-%m-01')
			and lt.transaction_date <= LAST_DAY(NOW())
			AND
			(mo.id = 1 OR 1 = -1)

			UNION ALL

			select
			ounder.id as officeId, ml.id, s.id as staffId,
            ROUND((ifnull(lt.principal_portion_derived,0) + ifnull(lt.interest_portion_derived,0) + ifnull(lt.fee_charges_portion_derived,0) + ifnull(lt.penalty_charges_portion_derived,0) + ifnull(lt.overpayment_portion_derived,0)), 2) as pif

			FROM m_office mo
			JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%')
			AND ounder.hierarchy like CONCAT('.', '%')
			join m_group c on c.office_id=ounder.id
			left join m_loan ml on c.id = ml.group_id
			left join m_loan_transaction lt on lt.loan_id = ml.id and lt.is_reversed = 0
			and lt.transaction_type_enum IN (2,8)
			left join m_staff s on ml.loan_officer_id = s.id

			where lt.transaction_date  >= DATE_FORMAT(NOW(),'%Y-%m-01')
			and lt.transaction_date <= LAST_DAY(NOW())
			AND
			(mo.id = 1 OR 1 = -1)


 ) as t

GROUP BY officeId,staffId,monthYear
order by monthYear","calculate ",0,0);