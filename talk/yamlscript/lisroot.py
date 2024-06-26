import ROOT

class Linear:
    def __call__(self, arr, par):
        return par[0] + arr[0]*par[1]

# create a linear function with offset 5,
# and pitch 2
l = Linear()
f = ROOT.TF1('pyf2', l, -1., 1., 2)
f.SetParameters(5., 2.)

# plot the function
c = ROOT.TCanvas()
f.Draw()
