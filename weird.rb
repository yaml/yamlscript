def divisors(n)
    divs = [1]
    divs2 = []

    i = 2
    while i * i <= n
        if n % i == 0 then
            j = n / i
            divs.append(i)
            if i != j then
                divs2.append(j)
            end
        end

        i = i + 1
    end

    divs2 += divs.reverse
    return divs2
end

def abundant(n, divs)
    return divs.sum > n
end

def semiperfect(n, divs)
    if divs.length > 0 then
        h = divs[0]
        t = divs[1..-1]
        if n < h then
            return semiperfect(n, t)
        else
            return n == h || semiperfect(n - h, t) || semiperfect(n, t)
        end
    else
        return false
    end
end

def sieve(limit)
    w = Array.new(limit, false)
    i = 2
    while i < limit
        if not w[i] then
            divs = divisors(i)
            if not abundant(i, divs) then
                w[i] = true
            else
                if semiperfect(i, divs) then
                    j = i
                    while j < limit
                        w[j] = true
                        j = j + i
                    end
                end
            end
        end
        i = i + 2
    end
    return w
end

def main
    w = sieve(17000)
    count = 0
    max = 25
    print "The first %d weird numbers:\n" % [max]
    n = 2
    while count < max
        if not w[n] then
            print n, " "
            count = count + 1
        end
        n = n + 2
    end
    print "\n"
end

main()
