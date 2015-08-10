#!/usr/bin/env ruby

require 'rubygems'
require 'pg'
require 'mysql'

PG_USERNAME = 'reporting'
PG_PASSWORD = 'reporting'
PG_HOST = 'localhost'
PG_DB = 'reporting'

MYSQL_USERNAME = 'strataproddw'
MYSQL_DB = 'strataprod'

MYSQL_PASSWORD = 'strataproddw'
MYSQL_HOST = 'rck-bmc-dbdw1.breakmycasino.com'
#MYSQL_PASSWORD = 'D&~4;<:qQvMgBSvk'
#MYSQL_HOST = 'rck-prd-dbdw1.yazino.com'

#LIMITER=' LIMIT 100000'
LIMITER=''

def exportEmailValidation()
  reporting_conn = PG.connect(host: PG_HOST, user: PG_USERNAME, password: PG_PASSWORD, dbname: PG_DB)
  mysql_conn = Mysql.connect(MYSQL_HOST, MYSQL_USERNAME, MYSQL_PASSWORD, MYSQL_DB)

  insert = 'INSERT INTO EMAIL_VALIDATION (EMAIL_ADDRESS,STATUS) VALUES($1, $2);'
  query = 'SELECT EMAIL_ADDRESS,STATUS FROM EMAIL_VALIDATION'+LIMITER

  puts 'query is: '+query
  puts 'insert is: '+insert
  reporting_conn.prepare('insert-email', insert)


    rs = mysql_conn.query(query)
    puts "got #{rs.num_rows} rows"

    rs.each_hash do |row|
      begin
      reporting_conn.exec_prepared('insert-email', [row['EMAIL_ADDRESS'], row['STATUS']]) unless row['EMAIL_ADDRESS'].nil?
      rescue Exception =>e
        row= row['EMAIL_ADDRESS']
        puts "error whilst trying to insert email #{row}, #{e}"
      end
    end

end

exportEmailValidation

