package modules

import com.google.inject.{AbstractModule, Provides}
import javax.inject.Singleton
import javax.sql.DataSource
import play.api.db.DBApi

class DatabaseModule extends AbstractModule {
  override def configure(): Unit = {}
  
  @Provides
  @Singleton
  def provideDataSource(dbApi: DBApi): DataSource = {
    dbApi.database("default").dataSource
  }
}
