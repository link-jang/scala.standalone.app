package tx.batesin.timeseries
import breeze.stats.distributions.Bernoulli
import breeze.stats.distributions._
import breeze.linalg.Counter
import breeze.linalg._
import org.apache.commons.math3.random.{MersenneTwister, RandomGenerator}
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicInteger

/** ����ֲ����� */

class PriorDistributions(pi: Double,k: Int,b: DenseVector[Double],x:DenseMatrix[Double],k_n: Double,w: Double,weight: Double,ss: Double){
  
  def r = PriorDistributions.draw_r(pi,k)
  def sigame2 = 1/PriorDistributions.draw_sigma_r(weight,ss)
  def b_r = PriorDistributions.gen_vector_b(r,b)
  def x_r = PriorDistributions.gen_oumu_r(r,x)
  println(r)
  println(b_r)
  println(x_r)
  def r_nozero = {
	 var i = 0
     r.foreach(e => if(e != 0.0){
     i = i + 1
  	})
  	i
  }
  
  def beta = PriorDistributions.draw_beta_sigmar(b_r,PriorDistributions.gene_Covariance(x_r,k_n,w,sigame2))
  
  def draw(): DenseVector[Double] = {
    val priorVector = DenseVector.zeros[Double](k + r_nozero + 1)
    priorVector(0) = sigame2
    priorVector(1 to k) := r
    priorVector(k + 1 to k + r_nozero) := beta 

    priorVector
  } 
}
object PriorDistributions {
  
  /** p(r)������ (r1,r2,...,rk)  */
  def draw_r(pi: Double,k: Int) : DenseVector[Double] = {
    val bernoulli_model = new Bernoulli(pi,new RandBasis(new ThreadLocalRandomGenerator(new MersenneTwister(System.currentTimeMillis()))))    
    val r_row = DenseVector.zeros[Double](k)
    for (i <- 0 to k-1){
      r_row(i) = if(bernoulli_model.draw()) 1 else 0   
    }
    r_row   
  }
  
  
  /**  p(sigma|r) ����   1/sigma^2 */
  def draw_sigma_r(weight: Double,ss: Double): Double = {
    val gamma_model = new Gamma(weight/2,2/ss)(new RandBasis(new ThreadLocalRandomGenerator(new MersenneTwister(System.currentTimeMillis()))))

    gamma_model.draw()
  }
  
  
  
  /** ����Э���� (oumu^-1)^-1 */
  def gene_Oumu(x:DenseMatrix[Double],k_n: Double,w: Double): DenseMatrix[Double] = {
    val muti_metrix = x.t * x
    (muti_metrix*w  +  diag(diag(muti_metrix)*(1 - w)))*k_n
  }
  
  
  
  def gene_Covariance(x:DenseMatrix[Double],k_n: Double,w: Double,sigma2: Double): DenseMatrix[Double] = {
    inv(gene_Oumu(x,k_n,w))*sigma2
  }
  /** ����  P(beta|sigma,r) (beta1,beta2,beta3...betak) */
  def draw_beta_sigmar(mean: DenseVector[Double],covariance : DenseMatrix[Double]): DenseVector[Double] = {
    println(covariance)
    println(mean)
  	val mutil_Gaussian_model = MultivariateGaussian(mean,covariance)(new RandBasis(new ThreadLocalRandomGenerator(new MersenneTwister(System.currentTimeMillis()))))
    mutil_Gaussian_model.draw()
  }
  
  def gen_oumu_r(r: DenseVector[Double],x:DenseMatrix[Double]): DenseMatrix[Double] = {
    val x_r = DenseMatrix.zeros[Double](x.rows, x.cols)
    var r_nozero_size = 0
    
    r.foreachPair((index,e) => if(e != 0.0){
      x_r(:: , r_nozero_size) := x(::,index)
      r_nozero_size = r_nozero_size + 1
    })
    
    x_r(::,0 to r_nozero_size - 1)
  }
  
  def gen_vector_b(r: DenseVector[Double],b: DenseVector[Double]): DenseVector[Double] = {
    val b_r = DenseVector.zeros[Double](r.length)
    var r_nozero_size = 0
    r.foreachPair((index,e) => if(e != 0.0){
      b_r.update(r_nozero_size, b(index))
      r_nozero_size = r_nozero_size + 1
    })

    b_r(0 to r_nozero_size - 1)
  }
  
  def priorDistr(pi: Double,k: Int,b: DenseVector[Double],x:DenseMatrix[Double],k_n: Double,w: Double,weight: Double,ss: Double): DenseVector[Double] = {

    
    val sigame2_1 = draw_sigma_r(weight,ss)
    val sigame2 = 1/draw_sigma_r(weight,ss)
    val r = draw_r(pi,k) 
    val b_r = gen_vector_b(r,b)
    val x_r = gen_oumu_r(r,x)
    var r_nozero_size = 0
    r.foreach(e => if(e != 0.0){
      r_nozero_size = r_nozero_size + 1
    })

    val beta = draw_beta_sigmar(b_r,gene_Covariance(x_r,k_n,w,sigame2))
    
    val priorVector = DenseVector.zeros[Double](k + r_nozero_size + 1)
    priorVector(0) = sigame2
    priorVector(1 to k) := r
    priorVector(k + 1 to k + r_nozero_size) := beta 

    priorVector
  }
    
  
  def predictive(parameter: Beta.Parameter) = new Polya(Counter(true->parameter._1,false->parameter._2))
  
  def  main(args: Array[String]): Unit={
    
    
	  val pi = 0.5
	  val k = 4
	  val b = DenseVector.zeros[Double](4)
	  val x = DenseMatrix((10.0,20.0,10.0,22.0),(11.0,21.0,10.0,20.0))
	  val k_n = 0.25
	  val w = 0.5
	  val weight =0.1
	  val ss = 1
//	  val out = priorDistr(pi,k,b,x,k_n,w,weight,ss)
//	  println(out)
	  
	  val out = new PriorDistributions(pi,k,b,x,k_n,w,weight,ss).draw
	  println(out)
  }
}
