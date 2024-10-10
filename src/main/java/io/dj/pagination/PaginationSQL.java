package io.dj.pagination;

public enum PaginationSQL {
	MARIA_DB("mariadb","%s LIMIT %d, %d"),
    MY_SQL("mysql", "%s LIMIT %d, %d"),
    // Oracle 12c 버전부터 사용 가능
    ORACLE("oracle", "%s OFFSET %d ROWS FETCH NEXT %d ROWS ONLY"),
    POSTGRESQL("postgresql", "%s OFFSET %d LIMIT %d"),
    SQL_SERVER("sqlserver", "%s OFFSET %d ROWS FETCH NEXT %d ROWS ONLY"),
    MS_SQL("mssql", "%s OFFSET %d ROWS FETCH NEXT %d ROWS ONLY");

	private final String dbms;
	private final String sql;
	
	PaginationSQL(String dbms, String sql){
		this.dbms = dbms;
		this.sql = sql;
	}

	public String getDbms() {
		return dbms;
	}

	public String getSql() {
		return sql;
	}
}
