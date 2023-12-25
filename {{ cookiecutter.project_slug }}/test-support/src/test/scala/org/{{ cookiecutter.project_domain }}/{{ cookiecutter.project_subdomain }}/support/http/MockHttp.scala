package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.support.http

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
