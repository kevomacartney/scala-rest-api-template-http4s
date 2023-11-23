<<<<<<<< HEAD:{{ cookiecutter.project_slug }}/test-support/src/main/scala/org/{{ cookiecutter.project_domain }}/{{ cookiecutter.project_subdomain }}/support/http/MockHttp.scala
package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.support.http
========
package org.{{ cookiecutter.project_subdomain }}.com.e2e.support.http
>>>>>>>> main:{{ cookiecutter.project_slug }}/test-support/src/main/scala/org/{{ cookiecutter.project_subdomain }}/com/e2e/support/http/MockHttp.scala

import java.net.ServerSocket

object MockHttp {

  protected def getFreePort: Int = {
    val socket = new ServerSocket(0)
    socket.setReuseAddress(true)

    val localPort = socket.getLocalPort
    socket.close()

    localPort
  }
}
