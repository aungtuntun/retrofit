/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit2.adapter.rxjava;

import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import rx.Single;
import rx.observers.TestSubscriber;

import static okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;

public final class SingleTest {
  @Rule public final MockWebServer server = new MockWebServer();

  interface Service {
    @GET("/") Single<String> body();
    @GET("/") Single<Response<String>> response();
    @GET("/") Single<Result<String>> result();
  }

  private Service service;

  @Before public void setUp() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new StringConverterFactory())
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .build();
    service = retrofit.create(Service.class);
  }

  @Test public void bodySuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    TestSubscriber<String> subscriber = new TestSubscriber<>();
    service.body().subscribe(subscriber);
    subscriber.assertValue("Hi");
  }

  @Test public void bodySuccess404() {
    server.enqueue(new MockResponse().setResponseCode(404));

    TestSubscriber<String> subscriber = new TestSubscriber<>();
    service.body().subscribe(subscriber);
    Throwable cause = subscriber.getOnErrorEvents().get(0);
    assertThat(cause).isInstanceOf(HttpException.class).hasMessage("HTTP 404 Client Error");
  }

  @Test public void bodyFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    TestSubscriber<String> subscriber = new TestSubscriber<>();
    service.body().subscribe(subscriber);
    subscriber.assertError(IOException.class);
  }

  @Test public void responseSuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    TestSubscriber<Response<String>> subscriber = new TestSubscriber<>();
    service.response().subscribe(subscriber);
    Response<String> response = subscriber.getOnNextEvents().get(0);
    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
  }

  @Test public void responseSuccess404() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

    TestSubscriber<Response<String>> subscriber = new TestSubscriber<>();
    service.response().subscribe(subscriber);
    Response<String> response = subscriber.getOnNextEvents().get(0);
    assertThat(response.isSuccessful()).isFalse();
    assertThat(response.errorBody().string()).isEqualTo("Hi");
  }

  @Test public void responseFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    TestSubscriber<Response<String>> subscriber = new TestSubscriber<>();
    service.response().subscribe(subscriber);
    subscriber.assertError(IOException.class);
  }

  @Test public void resultSuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    TestSubscriber<Result<String>> subscriber = new TestSubscriber<>();
    service.result().subscribe(subscriber);
    Result<String> result = subscriber.getOnNextEvents().get(0);
    assertThat(result.isError()).isFalse();
    Response<String> response = result.response();
    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
  }

  @Test public void resultSuccess404() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

    TestSubscriber<Result<String>> subscriber = new TestSubscriber<>();
    service.result().subscribe(subscriber);
    Result<String> result = subscriber.getOnNextEvents().get(0);
    assertThat(result.isError()).isFalse();
    Response<String> response = result.response();
    assertThat(response.isSuccessful()).isFalse();
    assertThat(response.errorBody().string()).isEqualTo("Hi");
  }

  @Test public void resultFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    TestSubscriber<Result<String>> subscriber = new TestSubscriber<>();
    service.result().subscribe(subscriber);
    Result<String> result = subscriber.getOnNextEvents().get(0);
    assertThat(result.isError()).isTrue();
    assertThat(result.error()).isInstanceOf(IOException.class);
  }
}
