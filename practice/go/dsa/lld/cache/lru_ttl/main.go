package main

import (
	"fmt"
	"time"

	"github.com/harryv2/interview-preperation/practice/go/dsa/lld/cache/lru_ttl/cache"
)

func main() {

	clock := cache.SystemClock{}
	lru := cache.NewLruCache[string, string](10, &clock)

	lru.Add("a", "a", time.Minute)

	fmt.Println(lru.Size())

}
