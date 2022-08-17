package modules

import com.google.inject.{AbstractModule, Provides}
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import configurations.ElasticConfiguration

class ElasticModule extends AbstractModule {
  @Provides
  def sksElasticClientProvider(
      elasticConfiguration: ElasticConfiguration
  ): ElasticClient = {
    ElasticClient(
      JavaClient(ElasticProperties(elasticConfiguration.endpoint))
    )
  }
}
