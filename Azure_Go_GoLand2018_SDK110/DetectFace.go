package main

import (
	"crypto/tls"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"strings"
	"time"
)

func main() {
	const subscriptionKey = "<Subscription Key>"
	const uriBase = "https://<My Endpoint String>.com/face/v1.0/detect"
	const imageUrl = "https://*.jpg"

	const params = "?returnFaceAttributes=age,gender,headPose,smile,facialHair," +
		"glasses,emotion,hair,makeup,occlusion,accessories,blur,exposure,noise"
	const uri = uriBase + params
	const imageUrlEnc = "{\"url\":\"" + imageUrl + "\"}"

	reader := strings.NewReader(imageUrlEnc)

	tr := &http.Transport{
		TLSClientConfig: &tls.Config{
			InsecureSkipVerify: true,
		},
	}

	client := &http.Client{
		Transport: tr,
		Timeout:   time.Second * 2,
	}

	req, err := http.NewRequest("POST", uri, reader)
	if err != nil {
		panic(err)
	}

	req.Header.Add("Content-Type", "application/json")
	req.Header.Add("Ocp-Apim-Subscription-Key", subscriptionKey)

	resp, err := client.Do(req)
	if err != nil {
		panic(err)
	}

	defer resp.Body.Close()

	data, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		panic(err)
	}

	var f interface{}
	json.Unmarshal(data, &f)

	jsonFormatted, _ := json.MarshalIndent(f, "", "  ")
	fmt.Println(string(jsonFormatted))
}
