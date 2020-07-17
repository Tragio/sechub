// SPDX-License-Identifier: MIT

package cli

import (
	"fmt"
	"io/ioutil"
	"net/http"
	"os"

	sechubUtil "daimler.com/sechub/util"
	sechubutil "daimler.com/sechub/util"
)

// HandleHTTPError handler method for http errors
func HandleHTTPError(err error) {
	if err != nil {
		sechubUtil.LogError(fmt.Sprintf("The HTTP request failed with error %s\n", err))
		os.Exit(ExitCodeHTTPError)
	}
}

// HandleError handler method for common errors
func HandleError(err error) {
	if err != nil {
		sechubUtil.LogError(fmt.Sprintf("Error: %s\n", err))
		os.Exit(ExitCodeFailed)
	}
}

// HandleHTTPResponse handler method for http response. when not 200 a error log entry will be created and sechub client does exit
func HandleHTTPResponse(res *http.Response) {
	if res.StatusCode != 200 {
		b, _ := ioutil.ReadAll(res.Body)
		sechubUtil.LogError(fmt.Sprintf("The HTTP request failed with error %s\nbody=%s\n", res.Status, string(b)))
		os.Exit(ExitCodeHTTPError)
	}
}

// HandleHTTPErrorAndResponse does just handle error and repsonse
func HandleHTTPErrorAndResponse(res *http.Response, err error, context *Context) {
	sechubutil.LogDebug(context.config.debug, fmt.Sprintf("HTTP response: %+v", res))
	HandleHTTPError(err)
	HandleHTTPResponse(res)
}
